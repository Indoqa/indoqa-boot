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

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.VersionProvider;

public abstract class AbstractSystemInfo {

    private String version;
    private boolean initialized;

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

    public String getVersion() {
        return this.version;
    }

    @PostConstruct
    public void initProperties() {
        this.version = initApplicationVersion(this.versionProvider);
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }
}
