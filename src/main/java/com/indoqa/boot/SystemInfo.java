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

import static java.lang.System.getenv;
import static java.util.stream.Collectors.toMap;
import static org.springframework.core.env.StandardEnvironment.*;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemInfo {

    private String version;
    private Date started;
    private long initializationDuration;
    private String javaVersion;
    private Map<String, String> systemProperties;
    private Map<String, SpringProperty> springProperties;
    private Map<String, String> javaEnvironment;
    private String[] profiles;
    private String port;
    private Map<String, String> more = new HashMap<>();
    private boolean initialized;

    @JsonIgnore
    @Inject
    private ConfigurableEnvironment springEnvironment;

    @JsonIgnore
    @Inject
    private VersionProvider versionProvider;

    private static TreeMap<String, String> createJavaEnvironmentMap() {
        return new TreeMap<>(getenv());
    }

    private static Map<String, SpringProperty> createSpringProperties(ConfigurableEnvironment springEnvironment) {
        Map<String, SpringProperty> springProperties = new HashMap<>();

        for (PropertySource<?> eachPropertySource : springEnvironment.getPropertySources()) {
            if (!(eachPropertySource instanceof EnumerablePropertySource)) {
                continue;
            }

            EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) eachPropertySource;
            for (String prop : enumerablePropertySource.getPropertyNames()) {
                String sourceName = enumerablePropertySource.getName();

                SpringProperty springProperty = springProperties.get(prop);
                if (springProperty == null) {
                    springProperty = new SpringProperty(prop);
                }

                springProperty.setValue(enumerablePropertySource.getProperty(prop), sourceName);
                springProperties.put(prop, springProperty);
            }
        }

        return filterSystemAndEnvironmentProperties(springProperties);
    }

    private static Map<String, String> createSystemPropertiesMap() {
        Properties systemProps = System.getProperties();
        Enumeration<Object> keys = systemProps.keys();

        Map<String, String> result = new TreeMap<>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            result.put(key, System.getProperty(key));
        }

        return result;
    }

    private static Predicate<? super Entry<String, SpringProperty>> filterSingleEntries(String sourceName) {
        return entry -> {
            String source = entry.getValue().getSource();
            boolean hasOverriddenProperties = entry.getValue().hasOverriddenProperties();
            return sourceName.equals(source) && !hasOverriddenProperties;
        };
    }

    private static Map<String, SpringProperty> filterSystemAndEnvironmentProperties(Map<String, SpringProperty> springProperties) {
        return new TreeMap<>(
            springProperties
                .entrySet()
                .stream()
                .filter(filterSingleEntries(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME).negate())
                .filter(filterSingleEntries(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME).negate())
                .collect(toMap(entry -> entry.getKey(), entry -> entry.getValue())));
    }

    private static String getAttribute(Class<?> archivedClass, String property) throws IOException {
        Manifest manifest = getManifest(archivedClass);
        if (manifest == null) {
            return null;
        }

        Attributes entries = manifest.getMainAttributes();
        return entries.getValue(property);
    }

    private static Manifest getManifest(Class<?> archivedClass) throws IOException {
        URL codeBase = archivedClass.getProtectionDomain().getCodeSource().getLocation();
        if (!codeBase.getPath().endsWith(".jar")) {
            return null;
        }

        JarInputStream jarInputStream = null;
        try {
            jarInputStream = new JarInputStream(codeBase.openStream());
            return jarInputStream.getManifest();
        } finally {
            if (jarInputStream != null) {
                jarInputStream.close();
            }
        }
    }

    public void addProperty(String key, String value) {
        this.more.put(key, value);
    }

    @JsonProperty("initialization-duration")
    public long getInitializationDuration() {
        return this.initializationDuration;
    }

    @JsonProperty("environment")
    public Map<String, String> getJavaEnvironment() {
        return this.javaEnvironment;
    }

    @JsonProperty("java-version")
    public String getJavaVersion() {
        return this.javaVersion;
    }

    public Map<String, String> getMore() {
        return this.more;
    }

    public String getPort() {
        return this.port;
    }

    public String[] getProfiles() {
        return this.profiles;
    }

    @JsonProperty("spring-properties")
    public Map<String, SpringProperty> getSpringProperties() {
        return this.springProperties;
    }

    public String getStarted() {
        if (this.started == null) {
            return null;
        }

        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ").format(this.started);
    }

    @JsonProperty("system-properties")
    public Map<String, String> getSystemProperties() {
        return this.systemProperties;
    }

    public String getVersion() {
        return this.version;
    }

    @PostConstruct
    public void initializeProperties() {
        this.version = this.getApplicationVersion();
        this.javaVersion = System.getProperty("java.version");
        this.profiles = this.getActiveProfiles();
        this.port = this.lookupPort();

        this.javaEnvironment = createJavaEnvironmentMap();
        this.systemProperties = createSystemPropertiesMap();
        this.springProperties = createSpringProperties(this.springEnvironment);
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitializationDuration(long duration) {
        this.initializationDuration = duration;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date started() {
        return this.started;
    }

    private String[] getActiveProfiles() {
        return this.springEnvironment.getActiveProfiles();
    }

    private String getApplicationVersion() {
        try {
            String versionAttribute = getAttribute(this.versionProvider.getClass(), "Implementation-Version");
            if (versionAttribute != null) {
                return versionAttribute;
            }

            versionAttribute = getAttribute(this.versionProvider.getClass(), "Implementation-Build");
            if (versionAttribute != null) {
                return versionAttribute;
            }

            return "UNKNOWN_VERSION";
        } catch (IOException e) {
            throw new ApplicationInitializationException("Cannot read from manifest.", e);
        }
    }

    private String lookupPort() {
        String result = this.springEnvironment.getProperty("port");
        if (StringUtils.isNotBlank(result)) {
            return result;
        }

        // default Spark port
        return "4567";
    }
}
