/*
 * Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Indoqa licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.indoqa.boot.application;

import static com.indoqa.boot.logging.InitializationLogger.getInitializationLogger;
import static com.indoqa.boot.profile.Profile.*;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.actuate.activators.ActuatorActivators;
import com.indoqa.boot.actuate.activators.DefaultActuatorActivator;
import com.indoqa.boot.actuate.resources.*;
import com.indoqa.boot.actuate.systeminfo.BasicSystemInfo;
import com.indoqa.boot.actuate.systeminfo.SystemInfo;
import com.indoqa.boot.json.interceptor.DefaultContentTypeAfterInterceptor;
import com.indoqa.boot.json.transformer.JacksonTransformer;
import com.indoqa.boot.spark.SparkAdminService;
import com.indoqa.boot.spark.SparkDefaultService;
import com.indoqa.boot.version.VersionProvider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.util.ClassUtils;

import spark.ResponseTransformer;
import spark.Spark;

/**
 * <p>
 * The {@link AbstractIndoqaBootApplication} is the entry point to Indoqa-Boot. Use the {@link #invoke()} or
 * {@link #invoke(StartupLifecycle)} methods to initialize the startup procedure of Spring and Spark.
 * </p>
 */
public abstract class AbstractIndoqaBootApplication implements VersionProvider {

    private static final Logger LOGGER = getLogger(AbstractIndoqaBootApplication.class);
    private static final Date START_TIME = new Date();

    private AnnotationConfigApplicationContext context;

    private SystemInfo systemInfo;
    private int beansHashCode;
    private String asciiLogo;

    private static ResourcePropertySource getProperties(String propertiesLocation) {
        try {
            return new ResourcePropertySource(propertiesLocation);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading properties from " + propertiesLocation, e);
        }
    }

    private static boolean isExternalPropertiesFileProvided() {
        return System.getProperty("properties") != null;
    }

    /**
     * Start the Indoqa-Boot application.
     */
    public void invoke() {
        this.invoke(NoopStartupLifecycle.INSTANCE);
    }

    /**
     * Start the Indoqa-Boot application and hook into the startup lifecycle.
     *
     * @param lifecycle Provide an implementation of the callback interface.
     */
    public void invoke(StartupLifecycle lifecycle) {
        this.initializeAsciiLogo();
        this.printLogo();

        lifecycle.willInitialize();
        this.logInitializationStart();

        lifecycle.willCreateSpringContext();
        this.initializeApplicationContext();

        lifecycle.didCreateSpringContext(this.context);
        this.initializeVersionProvider();
        this.initializeSystemInfo();
        this.initializeProfile();
        this.initializeExternalProperties();
        this.initializePropertyPlaceholderConfigurer();

        this.initializeSpark();
        lifecycle.willCreateDefaultSparkRoutes(this.context);

        this.initializeJsonTransformer();
        this.initializeDefaultResources();
        this.initializeActuators();

        lifecycle.willScanForComponents(this.context);
        this.initializeSpringComponentScan();

        lifecycle.willRefreshSpringContext(this.context);
        this.refreshApplicationContext();
        this.completeSystemInfoInitialization();
        lifecycle.didInitializeSpring(this.context);

        this.enableApplicationReloading();
        Optional<CharSequence> statusMessages = lifecycle.didInitialize();
        this.logInitializationFinished(statusMessages);
    }

    /**
     * If the log-path system property is not set or is invalid, the application fails hard.
     *
     * @return Return false if the check is not required. Default is true.
     */
    protected boolean checkLoggerInitialization() {
        return true;
    }

    /**
     * Activate actuators.
     *
     * @param actuatorActivators Use it to enable {@link ActuatorActivators}.
     */
    protected void enableActuators(ActuatorActivators actuatorActivators) {
        // do nothing
    }

    /**
     * Provide a custom application name. The default is the simple name of the class extending the this class.
     *
     * @return The custom application name.
     */
    protected String getApplicationName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Provide the path to a resource containing the ascii logo which will be printed to the console at startup. The resource is looked up
     * in the classpath.
     *
     * @return The resource path of the ascii logo.
     */
    protected String getAsciiLogoPath() {
        return null;
    }

    /**
     * Provide an array of package names which will be used by Spring to automatically detect Spring beans. The default is
     * <code>null</code>.
     *
     * @return An array of package names or null if this feature should be disabled.
     */
    protected String[] getComponentScanBasePackages() {
        return null;
    }

    /**
     * The class of the JSON transformer that should be used to marshal Java objects as JSON strings. The default is the
     * {@link JacksonTransformer}.
     *
     * @return The class of the response transformer.
     */
    protected Class<? extends ResponseTransformer> getJsonTransformerClass() {
        return JacksonTransformer.class;
    }

    /**
     * Provide an alternative {@link VersionProvider} which will be used to lookup the jar manifest that contains the
     * Implementation-Version. The version information is provided by the {@link SystemInfoResource} and is printed as part of the status
     * message printed to the console at startup.
     *
     * @return An alternative {@link VersionProvider}.
     */
    protected VersionProvider getVersionProvider() {
        return this;
    }

    /**
     * Indoqa-Boot uses this method to decided whether the application runs in production or development mode. The default implementation
     * detects if the application runs from within a runnable Java archive (jar) and if this is true, <code>false</code> is returned.
     *
     * @return True if the application should run in development mode.
     */
    protected boolean isDevEnvironment() {
        String javaCommand = System.getProperty("sun.java.command");
        return !StringUtils.endsWith(javaCommand, ".jar");
    }

    private void completeSystemInfoInitialization() {
        long duration = currentTimeMillis() - START_TIME.getTime();
        this.systemInfo = this.context.getBean(SystemInfo.class);
        this.systemInfo.setInitializationDuration(duration);
        this.systemInfo.setStarted(START_TIME);
        this.systemInfo.setInitialized(true);
        this.systemInfo.recheckForRandomlyAssignedPorts();
        this.systemInfo.setApplicationName(this.getApplicationName());
        this.systemInfo.setAsciiLogo(this.asciiLogo);

        BasicSystemInfo reducedSystemInfo = this.context.getBean(BasicSystemInfo.class);
        reducedSystemInfo.setInitialized(true);
    }

    private void enableApplicationReloading() {
        // no reloading for production environments
        if (!this.isDevEnvironment()) {
            return;
        }

        // check if Hotswap-Agent is available at all
        if (!this.isHotswapAgentInstalled()) {
            LOGGER.info(
                "Application reloading is NOT enabled. Install Hotswap Agent by following "
                    + "the instructions at https://github.com/HotswapProjects/HotswapAgent");
            return;
        }

        LOGGER.info("Application reloading is enabled based on Hotswap Agent.");
        this.beansHashCode = this.getBeansHashCode();
        // create a TimerTask that frequently checks if the Spring application context has changed
        new Timer().scheduleAtFixedRate(new ReloadingTimerTask(), 0, SECONDS.toMillis(1));
    }

    private int getBeansHashCode() {
        return Arrays
            .stream(this.context.getBeanFactory().getBeanDefinitionNames())
            .map(bd -> this.context.getBeanFactory().getBean(bd))
            .collect(Collectors.toList())
            .hashCode();
    }

    private boolean hasNoActiveProfile() {
        String[] activeProfiles = this.context.getEnvironment().getActiveProfiles();
        return activeProfiles == null || activeProfiles.length == 0;
    }

    private boolean haveSpringBeansNotChanged() {
        return this.beansHashCode == this.getBeansHashCode();
    }

    private void initializeActuators() {
        // register actuator activators
        ActuatorActivators actuatorActivators = new ActuatorActivators();
        actuatorActivators.enable(DefaultActuatorActivator.class);
        this.enableActuators(actuatorActivators);
        actuatorActivators.getActuatorActivators().forEach(this.context::register);

        // register actuator resources
        this.context.register(OverviewResources.class);
        this.context.register(SystemInfoResource.class);
        this.context.register(HealthResources.class);
        this.context.register(ThreadDumpResources.class);
        this.context.register(HeapDumpResources.class);
        this.context.register(MetricsResources.class);
        this.context.register(ActuatorGzipInterceptor.class);

        if (this.isClassAvailable("org.apache.logging.log4j.LogManager")) {
            this.context.register(Log4j2LoggingResource.class);
        }
    }

    private void initializeApplicationContext() {
        this.context = new AnnotationConfigApplicationContext();
    }

    private void initializeAsciiLogo() {
        String asciiLogoPath = this.getAsciiLogoPath();

        if (asciiLogoPath == null) {
            return;
        }

        try (InputStream asciiLogoInputStream = AbstractIndoqaBootApplication.class.getResourceAsStream(asciiLogoPath)) {
            this.asciiLogo = IOUtils.toString(asciiLogoInputStream, UTF_8);
        } catch (Exception e) {
            throw new ApplicationInitializationException("Error while reading ASCII logo from " + asciiLogoPath, e);
        }
    }

    private void initializeDefaultResources() {
        this.context.register(DefaultContentTypeAfterInterceptor.class);
        this.context.register(ShutdownResource.class);
    }

    private void initializeExternalProperties() {
        if (isExternalPropertiesFileProvided()) {
            String propertiesLocation = System.getProperty("properties");
            ResourcePropertySource propertySource = getProperties(propertiesLocation);
            this.context.getEnvironment().getPropertySources().addFirst(propertySource);
            LOGGER.info("Using external properties from {}", propertiesLocation);
        } else {
            LOGGER.info(
                "No external properties set. Use the system property 'properties' "
                    + "to provide application properties as a Java properties file.");
        }
    }

    private void initializeJsonTransformer() {
        this.context.register(this.getJsonTransformerClass());
    }

    private void initializeProfile() {
        if (this.hasNoActiveProfile()) {
            String detectedProfile = this.isDevEnvironment() ? DEV.getName() : PROD.getName();
            LOGGER.info("Explicitly set Spring profile: {}", detectedProfile);
            this.context.getEnvironment().setActiveProfiles(detectedProfile);
        }
        LOGGER.info("Active Spring profile(s): {}", join(" & ", this.context.getEnvironment().getActiveProfiles()));
    }

    private void initializePropertyPlaceholderConfigurer() {
        this.context.register(PropertySourcesPlaceholderConfigurer.class);
    }

    private void initializeSpark() {
        this.context.register(SparkAdminService.class);
        this.context.register(SparkDefaultService.class);
    }

    private void initializeSpringComponentScan() {
        String[] componentScanBasePackages = this.getComponentScanBasePackages();
        if (isNotEmpty(componentScanBasePackages)) {
            this.context.scan(componentScanBasePackages);
        }
    }

    private void initializeSystemInfo() {
        this.context.register(SystemInfo.class);
        this.context.register(BasicSystemInfo.class);
    }

    private void initializeVersionProvider() {
        this.context.getBeanFactory().registerSingleton(VersionProvider.class.getName(), this.getVersionProvider());
    }

    private boolean isClassAvailable(String className) {
        return ClassUtils.isPresent(className, this.context.getClassLoader());
    }

    private boolean isHotswapAgentInstalled() {
        try {
            this.getClass().getClassLoader().loadClass("org.hotswap.agent.HotswapAgent");
            return true;
        } catch (ClassNotFoundException e) { // NOSONAR
            return false;
        }
    }

    private void logInitializationFinished(Optional<CharSequence> additionalStatusMessages) {
        StringBuilder statusMessages = new StringBuilder()
            .append(this.getApplicationName())
            .append(" ")
            .append(this.systemInfo.getVersion())
            .append(" started at ")
            .append(this.systemInfo.getStarted())
            .append(" (initialized in ")
            .append(this.systemInfo.getInitializationDuration())
            .append(" ms")
            .append(", listening on port ")
            .append(this.systemInfo.getPort())
            .append(" and on admin-port ")
            .append(this.printAdminPort())
            .append(", active profile(s): ")
            .append(join("|", asList(this.systemInfo.getProfiles())))
            .append(", running on Java ")
            .append(this.systemInfo.getSystemProperties().get("java.version"))
            .append(" and Indoqa-Boot ")
            .append(this.systemInfo.getIndoqaBootVersion());
        additionalStatusMessages.ifPresent(message -> statusMessages.append(", ").append(message));
        statusMessages.append(")");

        LOGGER.info(statusMessages.toString());
        if (this.isDevEnvironment()) {
            return;
        }
        getInitializationLogger().info(statusMessages.toString());
    }

    private void logInitializationStart() {
        if (this.checkLoggerInitialization()) {
            LogPathValidator.checkLogDir();
        }

        LOGGER.info("Initializing " + this.getApplicationName());
    }

    private String printAdminPort() {
        String adminPort = this.systemInfo.getAdminPort();
        if (adminPort == null) {
            return "[n.a.]";
        }
        return adminPort;
    }

    private void printLogo() {
        if (isNotBlank(this.asciiLogo)) {
            getInitializationLogger().info(this.asciiLogo);
        }
    }

    private void refreshApplicationContext() {
        try {
            this.context.refresh();
        } catch (Exception e) {
            String msg = "An exception occurred while refreshing the Spring application context.";

            LOGGER.error(msg, e);
            getInitializationLogger().error(msg + " " + e.getMessage() + "\nPlease check the logs to get the stacktrace.\n");

            System.exit(1);
        }
    }

    private synchronized void reload() {
        if (this.haveSpringBeansNotChanged()) {
            return;
        }

        Spark.stop();
        this.context.close();
        this.invoke();
    }

    public class ReloadingTimerTask extends TimerTask {

        @Override
        public void run() {
            AbstractIndoqaBootApplication.this.reload();
        }
    }
}
