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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePropertySource;

import com.indoqa.boot.json.JacksonTransformer;

import spark.ResponseTransformer;
import spark.Spark;

public abstract class AbstractIndoqaBootApplication implements VersionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIndoqaBootApplication.class);
    private static final Logger INIT_LOGGER = LoggerFactory.getLogger(AbstractIndoqaBootApplication.class.getName() + "_INIT");

    private final Date START_TIME = new Date();

    private AnnotationConfigApplicationContext context;
    private ResourcePropertySource propertySource;
    private SystemInfo systemInfo;

    private int beansHashCode;

    protected static Logger getInitializationLogger() {
        return INIT_LOGGER;
    }

    public void invoke() {
        this.beforeInitialization();
        this.logInitializationStart();

        this.beforeSpringInitialization();
        this.initializeApplicationContext();
        this.initializeVersionProvider();
        this.initializeSystemInfo();
        this.initializeProfile();
        this.initializeExternalProperties();
        this.initializePropertyPlaceholderConfigurer();
        this.initializeSparkConfiguration();
        this.initializeJsonTransformer();
        this.initializeSpringBeans();
        this.initializeDefaultResources();
        this.initializeSpringComponentScan();
        this.refreshApplicationContext();
        this.completeSystemInfoInitialization();
        this.afterSpringInitialization();

        this.enableApplicationReloading();

        this.logInitializationFinished();
        this.afterInitialization();
    }

    protected void afterInitialization() {
        // empty implementation
    }

    protected void afterSpringInitialization() {
        // empty implementation
    }

    protected void beforeInitialization() {
        // empty implementation
    }

    protected void beforeSpringInitialization() {
        // empty implementation
    }

    protected boolean checkLoggerInitialization() {
        return true;
    }

    protected CharSequence getAdditionalStatusMessages() {
        return null;
    }

    protected AnnotationConfigApplicationContext getApplicationContext() {
        return this.context;
    }

    protected String getApplicationName() {
        return this.getClass().getSimpleName();
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

    protected void initializeSpringBeans() {
        // empty implementation
    }

    protected boolean isDevEnvironment() {
        return false;
    }

    private void completeSystemInfoInitialization() {
        long duration = System.currentTimeMillis() - this.START_TIME.getTime();
        this.systemInfo = this.context.getBean(SystemInfo.class);
        this.systemInfo.setInitializationDuration(duration);
        this.systemInfo.setStarted(this.START_TIME);
        this.systemInfo.setInitialized(true);
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
        List<String> beanDefinitions = Arrays.asList(this.getApplicationContext().getBeanFactory().getBeanDefinitionNames());
        List<Object> beans = beanDefinitions
            .stream()
            .map(bd -> this.getApplicationContext().getBeanFactory().getBean(bd))
            .collect(Collectors.toList());
        return beans.hashCode();
    }

    private ResourcePropertySource getProperties(String propertiesLocation) {
        try {
            return new ResourcePropertySource(propertiesLocation);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading properties from " + propertiesLocation, e);
        }
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
    }

    private void initializeExternalProperties() {
        if (this.isExternalPropertiesFileProvided()) {
            String propertiesLocation = System.getProperty("properties");
            this.propertySource = this.getProperties(propertiesLocation);
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
            String detectedProfile = this.isDevEnvironment() ? "dev" : "prod";
            LOGGER.info("Explicitly set Spring profile: {}", detectedProfile);
            this.context.getEnvironment().setActiveProfiles(detectedProfile);
        }
        LOGGER.info("Active Spring profile(s): {}", String.join(" & ", this.context.getEnvironment().getActiveProfiles()));
    }

    private void initializePropertyPlaceholderConfigurer() {
        this.getApplicationContext().register(PropertySourcesPlaceholderConfigurer.class);
    }

    private void initializeSparkConfiguration() {
        this.getApplicationContext().register(SparkConfiguration.class);
    }

    private void initializeSpringComponentScan() {
        this.context.scan(AbstractIndoqaBootApplication.class.getPackage().getName());
        this.context.scan(this.getComponentScanBasePackages());
    }

    private void initializeSystemInfo() {
        this.context.register(SystemInfo.class);
    }

    private void initializeVersionProvider() {
        this.context.getBeanFactory().registerSingleton(VersionProvider.class.getName(), this.getVersionProvider());
    }

    private boolean isDevProfileEnabled() {
        return ArrayUtils.contains(this.context.getEnvironment().getActiveProfiles(), "dev");
    }

    private boolean isExternalPropertiesFileProvided() {
        return System.getProperty("properties") != null;
    }

    private boolean isHotswapAgentInstalled() {
        try {
            this.getClass().getClassLoader().loadClass("org.hotswap.agent.HotswapAgent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void logInitializationFinished() {
        if (this.isDevProfileEnabled()) {
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
            .append(String.join("|", Arrays.asList(this.systemInfo.getProfiles())))
            .append(", running on Java ")
            .append(this.systemInfo.getJavaVersion());

        CharSequence additionalStatusMessages = this.getAdditionalStatusMessages();
        if (additionalStatusMessages != null) {
            statusMessages.append(additionalStatusMessages);
        }

        statusMessages.append(")");

        INIT_LOGGER.info(statusMessages.toString());
    }

    private void logInitializationStart() {
        if (this.checkLoggerInitialization()) {
            LogPathValidator.checkLogDir();
        }

        LOGGER.info("Initializing " + this.getApplicationName());
    }

    private void refreshApplicationContext() {
        this.context.refresh();
    }

    private synchronized void reload() {
        if (this.haveSpringBeansNotChanged()) {
            return;
        }

        Spark.stop();
        this.getApplicationContext().close();
        this.invoke();
    }

    public class ReloadingTimerTask extends TimerTask {

        @Override
        public void run() {
            AbstractIndoqaBootApplication.this.reload();
        }
    }

    public static class SparkConfiguration {

        @Inject
        private Environment environment;

        @PostConstruct
        public void initializeSpark() {
            String port = this.environment.getProperty("port");
            if (StringUtils.isNotBlank(port)) {
                Spark.port(Integer.parseInt(port));
            }
        }
    }
}
