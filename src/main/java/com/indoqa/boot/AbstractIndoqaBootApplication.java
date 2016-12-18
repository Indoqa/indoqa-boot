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
package com.indoqa.boot;

import static com.indoqa.boot.profile.Profile.*;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.support.ResourcePropertySource;

import com.indoqa.boot.json.DefaultContentTypeAfterInterceptor;
import com.indoqa.boot.json.JacksonTransformer;
import com.indoqa.boot.lifecycle.NoopStartupLifecycle;
import com.indoqa.boot.lifecycle.StartupLifecycle;
import com.indoqa.boot.resource.ShutdownResource;
import com.indoqa.boot.resource.SystemInfoResource;
import com.indoqa.boot.spark.SparkAdminService;
import com.indoqa.boot.spark.SparkDefaultService;
import com.indoqa.boot.systeminfo.BasicSystemInfo;
import com.indoqa.boot.systeminfo.SystemInfo;

import spark.ResponseTransformer;
import spark.Spark;

public abstract class AbstractIndoqaBootApplication implements VersionProvider {

    private static final Logger INIT_LOGGER = LoggerFactory.getLogger(AbstractIndoqaBootApplication.class.getName() + "_INIT");
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndoqaBootApplication.class);

    private static final Date START_TIME = new Date();

    private AnnotationConfigApplicationContext context;
    private ResourcePropertySource propertySource;

    private SystemInfo systemInfo;
    private int beansHashCode;

    public static Logger getInitializationLogger() {
        return INIT_LOGGER;
    }

    protected static Logger getLogger() {
        return LOGGER;
    }

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

    public void invoke() {
        this.invoke(NoopStartupLifecycle.INSTANCE);
    }

    public void invoke(StartupLifecycle lifecycle) {
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
        lifecycle.willCreateDefaultSparkRoutes();

        this.initializeJsonTransformer();
        this.initializeDefaultResources();

        lifecycle.willScanForComponents();
        this.initializeSpringComponentScan();

        lifecycle.willRefreshSpringContext();
        this.refreshApplicationContext();
        this.completeSystemInfoInitialization();
        lifecycle.didInitializeSpring();

        this.enableApplicationReloading();
        Optional<CharSequence> statusMessages = lifecycle.didInitialize();
        this.logInitializationFinished(statusMessages);
    }

    protected boolean checkLoggerInitialization() {
        return true;
    }

    protected CharSequence getAdditionalStatusMessages() {
        return null;
    }

    protected String getApplicationName() {
        return this.getClass().getSimpleName();
    }

    protected String getAsciiLogoPath() {
        return null;
    }

    protected String[] getComponentScanBasePackages() {
        return new String[] {this.getClass().getPackage().getName()};
    }

    protected Class<? extends ResponseTransformer> getJsonTransformerClass() {
        return JacksonTransformer.class;
    }

    protected VersionProvider getVersionProvider() {
        return this;
    }

    protected boolean isDevEnvironment() {
        return false;
    }

    private void completeSystemInfoInitialization() {
        long duration = currentTimeMillis() - START_TIME.getTime();
        this.systemInfo = this.context.getBean(SystemInfo.class);
        this.systemInfo.setInitializationDuration(duration);
        this.systemInfo.setStarted(START_TIME);
        this.systemInfo.setInitialized(true);
        this.systemInfo.recheckRandomPort();

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
        List<String> beanDefinitions = asList(this.context.getBeanFactory().getBeanDefinitionNames());
        List<Object> beans = beanDefinitions
            .stream()
            .map(bd -> this.context.getBeanFactory().getBean(bd))
            .collect(Collectors.toList());
        return beans.hashCode();
    }

    private boolean hasNoActiveProfile() {
        String[] activeProfiles = this.context.getEnvironment().getActiveProfiles();
        return activeProfiles == null || activeProfiles.length == 0;
    }

    private boolean haveSpringBeansNotChanged() {
        return this.beansHashCode == this.getBeansHashCode();
    }

    private void initializeApplicationContext() {
        this.context = new AnnotationConfigApplicationContext();
    }

    private void initializeDefaultResources() {
        this.context.register(SystemInfoResource.class);
        this.context.register(DefaultContentTypeAfterInterceptor.class);
        this.context.register(ShutdownResource.class);
    }

    private void initializeExternalProperties() {
        if (isExternalPropertiesFileProvided()) {
            String propertiesLocation = System.getProperty("properties");
            this.propertySource = getProperties(propertiesLocation);
            this.context.getEnvironment().getPropertySources().addFirst(this.propertySource);
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
        this.context.register(SparkDefaultService.class);
        this.context.register(SparkAdminService.class);
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

    private boolean isHotswapAgentInstalled() {
        try {
            this.getClass().getClassLoader().loadClass("org.hotswap.agent.HotswapAgent");
            return true;
        } catch (ClassNotFoundException e) { // NOSONAR
            return false;
        }
    }

    private void logInitializationFinished(Optional<CharSequence> additionalStatusMessages) {
        if (this.isDevEnvironment()) {
            return;
        }

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
            .append(", active profile(s): ")
            .append(join("|", asList(this.systemInfo.getProfiles())))
            .append(", running on Java ")
            .append(this.systemInfo.getSystemProperties().get("java.version"));

        additionalStatusMessages.ifPresent(message -> statusMessages.append(", ").append(message));

        statusMessages.append(")");

        INIT_LOGGER.info(statusMessages.toString());
    }

    private void logInitializationStart() {
        if (this.checkLoggerInitialization()) {
            LogPathValidator.checkLogDir();
        }

        LOGGER.info("Initializing " + this.getApplicationName());
    }

    private void printLogo() {
        String asciiLogoPath = this.getAsciiLogoPath();

        if (asciiLogoPath == null) {
            return;
        }

        try (InputStream asciiLogoInputStream = AbstractIndoqaBootApplication.class.getResourceAsStream(asciiLogoPath)) {
            String asciiLogo = IOUtils.toString(asciiLogoInputStream, UTF_8);
            if (asciiLogo == null) {
                return;
            }
            getInitializationLogger().info(asciiLogo);
        } catch (Exception e) {
            throw new ApplicationInitializationException("Error while reading ASCII logo from " + asciiLogoPath, e);
        }
    }

    private void refreshApplicationContext() {
        try {
            this.context.refresh();
        } catch (Exception e) {
            LOGGER.error("An exception occurred while refreshing the Spring application context.", e);
            this.context.close();
            Spark.stop();
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
