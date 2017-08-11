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
package com.indoqa.boot.actuate.systeminfo;

import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.version.VersionProvider;

public abstract class AbstractSystemInfo {

    private static final String GIT_PREFIX = "git.";

    private String version;
    private boolean initialized;
    private Map<String, String> git;

    @JsonIgnore
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

    private static String getKey(Entry<Object, Object> entry) {
        return StringUtils.substringAfter((String) entry.getKey(), GIT_PREFIX);
    }

    private static Manifest getManifest(Class<?> archivedClass) throws IOException {
        URL codeBase = archivedClass.getProtectionDomain().getCodeSource().getLocation();
        if (!codeBase.getPath().endsWith(".jar")) {
            return null;
        }

        try (JarInputStream jarInputStream = new JarInputStream(codeBase.openStream())) {
            return jarInputStream.getManifest();
        }
    }

    private static String initApplicationVersion(VersionProvider versionProvider) {
        try {
            String versionAttribute = getAttribute(versionProvider.getClass(), "Implementation-Version");
            if (versionAttribute != null) {
                return versionAttribute;
            }

            versionAttribute = getAttribute(versionProvider.getClass(), "Implementation-Build");
            if (versionAttribute != null) {
                return versionAttribute;
            }

            return "UNKNOWN_VERSION";
        } catch (IOException e) {
            throw new ApplicationInitializationException("Cannot read from manifest.", e);
        }
    }

    public Map<String, String> getGit() {
        return this.git;
    }

    public String getVersion() {
        return this.version;
    }

    @PostConstruct
    public void initProperties() {
        this.version = initApplicationVersion(this.versionProvider);
        this.git = this.initGitProperties();
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    protected boolean filterGitProperty(@SuppressWarnings("unused") Object object) {
        return true;
    }

    private Map<String, String> filterGitPropertiesInternal(Properties allGitProperties) {
        return allGitProperties.entrySet().stream().filter(entry -> this.filterGitProperty(entry.getKey())).collect(
            toMap(entry -> getKey(entry), entry -> (String) entry.getValue()));
    }

    private Map<String, String> initGitProperties() {
        URL gitPropertiesUrl = AbstractSystemInfo.class.getResource("/git.properties");
        if (gitPropertiesUrl == null) {
            return null;
        }

        try (InputStream gitPropertiesInputStream = gitPropertiesUrl.openStream()) {
            Properties gitProperties = new Properties();
            gitProperties.load(gitPropertiesInputStream);
            return this.filterGitPropertiesInternal(gitProperties);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading git.properties from classpath.", e);
        }
    }
}
