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

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SystemInfo {

    private String version;
    private Date started;
    private long initializationDuration;
    private String javaVersion;
    private String[] profiles;
    private String port;
    private Map<String, String> more = new HashMap<>();
    private boolean initialized;

    @JsonIgnore
    @Inject
    private Environment environment;

    @Inject
    private VersionProvider versionProvider;

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

    public String getStarted() {
        if (this.started == null) {
            return null;
        }

        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZ").format(this.started);
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
        return this.environment.getActiveProfiles();
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
        String result = this.environment.getProperty("port");
        if (StringUtils.isNotBlank(result)) {
            return result;
        }

        // default Spark port
        return "4567";
    }
}
