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
package com.indoqa.boot.systeminfo;

import static java.lang.System.getenv;
import static java.util.Locale.US;
import static java.util.stream.Collectors.toMap;
import static org.springframework.core.env.StandardEnvironment.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.indoqa.boot.spark.SparkAdminService;
import com.indoqa.boot.version.VersionProvider;

import spark.Service;

public class SystemInfo extends AbstractSystemInfo {

    private Date started;
    private long initializationDuration;
    private Map<String, String> systemProperties;
    private Map<String, SpringProperty> springProperties;
    private Map<String, String> javaEnvironment;
    private List<String> springPropertySources;
    private String[] profiles;
    private String port;
    private String adminPort;
    private Map<String, String> more = new HashMap<>();

    @JsonIgnore
    @Inject
    private ConfigurableEnvironment springEnvironment;

    @JsonIgnore
    @Inject
    private VersionProvider versionProvider;

    @JsonIgnore
    @Inject
    private SparkAdminService sparkAdminService;

    private static Map<String, SpringProperty> filterSystemAndEnvironmentProperties(Map<String, SpringProperty> springProperties) {
        return new TreeMap<>(
            springProperties
                .entrySet()
                .stream()
                .filter(isSingleEntryOfSource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME).negate())
                .filter(isSingleEntryOfSource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME).negate())
                .collect(toMap(entry -> entry.getKey(), entry -> entry.getValue())));
    }

    private static String[] initActiveProfiles(ConfigurableEnvironment springEnvironment) {
        return springEnvironment.getActiveProfiles();
    }

    private static String initAdminPort(ConfigurableEnvironment springEnvironment) {
        return springEnvironment.getProperty("admin-port");
    }

    private static Map<String, String> initJavaEnvironmentMap() {
        return new TreeMap<>(getenv());
    }

    private static String initPort(ConfigurableEnvironment springEnvironment) {
        return springEnvironment.getProperty("port");
    }

    private static Map<String, SpringProperty> initSpringProperties(ConfigurableEnvironment springEnvironment) {
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

    private static List<String> initSpringPropertySources(ConfigurableEnvironment env) {
        return StreamSupport
            .stream(env.getPropertySources().spliterator(), false)
            .map(source -> source.getName())
            .collect(Collectors.toList());
    }

    private static Map<String, String> initSystemPropertiesMap() {
        Properties systemProps = System.getProperties();
        Enumeration<Object> keys = systemProps.keys();

        Map<String, String> result = new TreeMap<>();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement().toString();
            result.put(key, System.getProperty(key));
        }

        return result;
    }

    private static Predicate<? super Entry<String, SpringProperty>> isSingleEntryOfSource(String sourceName) {
        return entry -> {
            String source = entry.getValue().getSource();
            boolean hasOverriddenProperties = entry.getValue().hasOverriddenProperties();
            return sourceName.equals(source) && !hasOverriddenProperties;
        };
    }

    public void addProperty(String key, String value) {
        this.more.put(key, value);
    }

    public String getAdminPort() {
        return this.adminPort;
    }

    @JsonProperty("initialization-duration")
    public long getInitializationDuration() {
        return this.initializationDuration;
    }

    @JsonProperty("environment")
    public Map<String, String> getJavaEnvironment() {
        return this.javaEnvironment;
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

    public List<String> getSpringPropertySources() {
        return this.springPropertySources;
    }

    public String getStarted() {
        if (this.started == null) {
            return null;
        }

        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ", US).format(this.started);
    }

    @JsonProperty("system-properties")
    public Map<String, String> getSystemProperties() {
        return this.systemProperties;
    }

    @Override
    @PostConstruct
    public void initProperties() {
        super.initProperties();
        this.profiles = initActiveProfiles(this.springEnvironment);
        this.port = initPort(this.springEnvironment);
        this.adminPort = initAdminPort(this.springEnvironment);

        this.javaEnvironment = initJavaEnvironmentMap();
        this.systemProperties = initSystemPropertiesMap();
        this.springProperties = initSpringProperties(this.springEnvironment);
        this.springPropertySources = initSpringPropertySources(this.springEnvironment);
    }

    public void recheckForRandomlyAssignedPorts() {
        this.port = Integer.toString(JettyPortReader.getPort());

        Service adminServiceInstance = this.sparkAdminService.instance();
        if (adminServiceInstance != null) {
            this.adminPort = Integer.toString(JettyPortReader.getAdminPort(adminServiceInstance));
        }
    }

    public void setInitializationDuration(long duration) {
        this.initializationDuration = duration;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public Date started() {
        return this.started;
    }
}
