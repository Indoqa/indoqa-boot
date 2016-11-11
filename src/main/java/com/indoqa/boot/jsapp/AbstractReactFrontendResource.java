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
package com.indoqa.boot.jsapp;

import static com.indoqa.boot.jsapp.WebpackAssetsUtils.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static spark.Spark.*;

import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.core.env.Environment;

import spark.Spark;

public abstract class AbstractReactFrontendResource extends AbstractJsAppResourcesBase {

    private static final long EXPIRE_TIME = DAYS.toSeconds(1000);
    private static final String DEV_PROFILE = "dev";

    @Inject
    private Environment environment;

    private final String mountPath;
    private final String classPathLocation;
    private final String fileSystemLocation;

    public AbstractReactFrontendResource(String mountPath, String classPathLocation, String fileSystemLocation) {
        this.mountPath = mountPath;
        this.classPathLocation = classPathLocation;
        this.fileSystemLocation = fileSystemLocation;
    }

    @PostConstruct
    public void mount() {
        if (this.isDev()) {
            this.mountFrontendFromFilesystem();
        } else {
            this.mountFrontendFromClasspath();
        }
    }

    protected ProxyURLMappings getProxyURLMappings() {
        return new ProxyURLMappings();
    }

    private boolean isDev() {
        return Arrays.stream(this.environment.getActiveProfiles()).anyMatch(profile -> profile.equals(DEV_PROFILE));
    }

    private void mountFrontendFromClasspath() {
        staticFileLocation(this.classPathLocation);
        Spark.staticFiles.expireTime(EXPIRE_TIME);

        this.jsApp(this.mountPath, findWebpackAssetsInClasspath(this.classPathLocation), this.getProxyURLMappings());

    }

    private void mountFrontendFromFilesystem() {
        externalStaticFileLocation(this.fileSystemLocation);

        this.jsApp(this.mountPath, findWebpackAssetsInFilesystem(this.fileSystemLocation), this.getProxyURLMappings());
    }

}
