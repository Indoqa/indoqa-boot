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

import static java.util.concurrent.TimeUnit.*;

import java.util.Date;
import javax.inject.Inject;

import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.profile.ProfileDetector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import spark.Request;
import spark.Response;
import spark.Spark;
import spark.staticfiles.StaticFilesConfiguration;

/**
 * Use this base implementation for STATIC React/Redux single-page applications that are build by e.g. CreateReactApp.
 */
public abstract class AbstractStaticReactResourceBase extends AbstractHtmlResourcesBase {

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

    protected void html(String classPathLocation, String fileSystemLocation) {
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();

        if (ProfileDetector.isDev(this.environment)) {
            staticHandler.configureExternal(fileSystemLocation);
        }
        else {
            staticHandler.configure(classPathLocation);
        }

        Spark.after((request, response) -> {
            setExpiryHeaders(request, response);
            staticHandler.consume(request.raw(), response.raw());
        });
    }
}
