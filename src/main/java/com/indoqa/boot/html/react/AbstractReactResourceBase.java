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
package com.indoqa.boot.html.react;

import static java.util.concurrent.TimeUnit.DAYS;
import static spark.Spark.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.springframework.core.env.Environment;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.profile.ProfileDetector;

import spark.Spark;

public abstract class AbstractReactResourceBase extends AbstractHtmlResourcesBase {

    private static final long EXPIRE_TIME = DAYS.toSeconds(1000);

    @Inject
    private Environment environment;

    private static void checkFileSystemLocation(Path fileSystemLocationPath) {
        if (!Files.exists(fileSystemLocationPath)) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " does not exist.");
        }
        if (!Files.isDirectory(fileSystemLocationPath)) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " is not a directory.");
        }
        if (!Files.isReadable(fileSystemLocationPath)) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " is not readable.");
        }
        if (fileSystemLocationPath.toFile().list().length == 0) {
            throw new ApplicationInitializationException(
                "The fileSystemLocation " + fileSystemLocationPath.toAbsolutePath() + " does not contain any resources.");
        }
    }

    private static void configureClasspathAssets(String mountPath, String classPathLocation, ReactHtmlBuilder htmlBuilder) {
        Spark.staticFiles.expireTime(EXPIRE_TIME);

        staticFileLocation(classPathLocation);

        WebpackAssetsUtils
            .findWebpackAssetsInClasspath(mountPath, classPathLocation, htmlBuilder::mainCssPath, htmlBuilder::mainJavascriptPath);
    }

    private static void configureFileSystemAssets(String mountPath, String fileSystemLocation, ReactHtmlBuilder htmlBuilder) {
        Path fileSystemLocationPath = Paths.get(fileSystemLocation);
        checkFileSystemLocation(fileSystemLocationPath);

        externalStaticFileLocation(fileSystemLocation);

        WebpackAssetsUtils
            .findWebpackAssetsInFilesystem(mountPath, fileSystemLocation, htmlBuilder::mainCssPath, htmlBuilder::mainJavascriptPath);
    }

    protected void configureHtmlBuilder(@SuppressWarnings("unused") ReactHtmlBuilder reactHtmlBuilder) {
        // default does nothing
    }

    protected ReactHtmlBuilder createHtmlBuilder() {
        return new ReactHtmlBuilder();
    }

    protected void html(String mountPath, String classPathLocation, String fileSystemLocation) {
        ReactHtmlBuilder htmlBuilder = this.createHtmlBuilder();

        if (ProfileDetector.isDev(this.environment)) {
            configureFileSystemAssets(mountPath, fileSystemLocation, htmlBuilder);
        } else {
            configureClasspathAssets(mountPath, classPathLocation, htmlBuilder);
        }

        this.configureHtmlBuilder(htmlBuilder);

        this.html(mountPath + "/*", htmlBuilder);
    }
}
