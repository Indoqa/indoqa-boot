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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import javax.inject.Inject;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.profile.ProfileDetector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import spark.Request;
import spark.Response;
import spark.Spark;
import spark.staticfiles.StaticFilesConfiguration;

/**
 * Use this base implementation for STATIC React/Redux single-page applications that are build by e.g. CreateReactApp.
 */
public abstract class AbstractStaticReactResourceBase extends AbstractHtmlResourcesBase {

    private static final Logger LOGGER = getLogger(AbstractStaticReactResourceBase.class);

    private static final String RESPONSE_HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String RESPONSE_HEADER_EXPIRES = "Expires";

    private static final long LONG_EXPIRY_TIME_SECONDS = DAYS.toSeconds(1000);
    private static final long LONG_EXPIRE_TIME_MS = LONG_EXPIRY_TIME_SECONDS * 1000;

    private static final long SHORT_EXPIRY_TIME_SECONDS = HOURS.toSeconds(1);
    private static final long SHORT_EXPIRE_TIME_MS = SHORT_EXPIRY_TIME_SECONDS * 1000;

    @Inject
    private Environment environment;

    private static void setExpiryHeaders(Request request, Response response) {
        String path = request.pathInfo();

        // no caching for HTML resources and dynamically created responses (e.g. JSON)
        if (response.body() != null || StringUtils.startsWith(request.headers("Accept"), "text/html")) {
            response.header(RESPONSE_HEADER_CACHE_CONTROL, "no-store, must-revalidate");
            response.header(RESPONSE_HEADER_EXPIRES, "0");
        }

        // long caching for Javascript and CSS files
        else if (StringUtils.endsWith(path, ".js") || StringUtils.endsWith(path, ".css")) {
            response.header(RESPONSE_HEADER_CACHE_CONTROL, "private, max-age=" + LONG_EXPIRY_TIME_SECONDS);
            response.header(RESPONSE_HEADER_EXPIRES, new Date(System.currentTimeMillis() + LONG_EXPIRE_TIME_MS).toString());
        }

        // everything else cache for a short period of time
        else {
            response.header(RESPONSE_HEADER_CACHE_CONTROL, "private, max-age=" + SHORT_EXPIRY_TIME_SECONDS);
            response.header(RESPONSE_HEADER_EXPIRES, new Date(System.currentTimeMillis() + SHORT_EXPIRE_TIME_MS).toString());
        }
    }

    private static void sendIndexHtml(String indexHtml, Response response) {
        response.body(indexHtml);
        response.header("Content-Type", "text/html; charset=utf-8");
    }

    /*
     * TODO response modifier
     * TODO initial state
     */
    protected void html(String classPathLocation, String fileSystemLocation) {
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        final String indexHtml = readIndexHtml(classPathLocation, fileSystemLocation);

        if (ProfileDetector.isDev(this.environment)) {
            staticHandler.configureExternal(fileSystemLocation);
        }
        else {
            staticHandler.configure(classPathLocation);
        }

        Spark.after((request, response) -> {
            // always set expiry headers
            setExpiryHeaders(request, response);

            // if some Spark resource has already produced a result, stop here
            if (StringUtils.isNotEmpty(request.body())) {
                return;
            }

            String pathInfo = request.pathInfo();

            // request to the root path
            if ("/".equals(pathInfo)) {
                sendIndexHtml(indexHtml, response);
            }

            // look for static resources
            boolean staticResourceSent = staticHandler.consume(request.raw(), response.raw());

            // otherwise send the index.html
            if (!staticResourceSent) {
                sendIndexHtml(indexHtml, response);
            }
        });
    }

    private String readIndexHtml(String classPathLocation, String fileSystemLocation) {
        if (ProfileDetector.isDev(this.environment)) {
            File filesystemIndexHtml = new File(fileSystemLocation, "index.html");

            if (!filesystemIndexHtml.exists()) {
                LOGGER.warn("There was no index.html found: " + filesystemIndexHtml.getAbsolutePath());
                return "<html><head><title>Error: index.html missing</title></head><body>Error: index.html missing</body></html>";
            }

            try {
                return FileUtils.readFileToString(filesystemIndexHtml, UTF_8);
            } catch (IOException e) {
                throw new ApplicationInitializationException(
                    "Error while reading index.html from the file system: " + filesystemIndexHtml.getAbsolutePath());
            }
        }
        String classpathIndexHtml = classPathLocation + "/index.html";
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream(classpathIndexHtml), UTF_8);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading index.html from classpath: " + classpathIndexHtml);
        }
    }
}
