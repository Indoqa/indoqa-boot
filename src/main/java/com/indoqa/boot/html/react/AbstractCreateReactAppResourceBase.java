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

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.html.resources.HtmlResponseModifier;
import com.indoqa.boot.json.transformer.HtmlEscapingAwareJsonTransformer;
import com.indoqa.boot.json.transformer.HtmlEscapingJacksonTransformer;
import com.indoqa.boot.profile.ProfileDetector;

import spark.Request;
import spark.Response;
import spark.Spark;
import spark.staticfiles.StaticFilesConfiguration;

/**
 * Use this base implementation for React/Redux single-page applications that are build with CreateReactApp.
 */
public abstract class AbstractCreateReactAppResourceBase extends AbstractHtmlResourcesBase {

    private static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String RESPONSE_HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String RESPONSE_HEADER_EXPIRES = "Expires";
    private static final long LONG_EXPIRY_TIME_SECONDS = DAYS.toSeconds(1000);
    private static final long LONG_EXPIRE_TIME_MS = LONG_EXPIRY_TIME_SECONDS * 1000;
    private static final long SHORT_EXPIRY_TIME_SECONDS = HOURS.toSeconds(1);
    private static final long SHORT_EXPIRE_TIME_MS = SHORT_EXPIRY_TIME_SECONDS * 1000;
    private static final HtmlEscapingAwareJsonTransformer TRANSFORMER = new HtmlEscapingJacksonTransformer();

    @Inject
    private Environment environment;

    private static StaticFilesConfiguration createStaticHandler(String classPathLocation, String fileSystemLocation, boolean isDev) {
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        if (isDev) {
            staticHandler.configureExternal(fileSystemLocation);
        } else {
            staticHandler.configure(classPathLocation);
        }
        return staticHandler;
    }

    private static void sendIndexHtml(Request request, Response response, IndexHtmlBuilder indexHtmlBuilder,
            InitialStateProvider initialStateProvider, HtmlResponseModifier responseModifier) {
        String indexHtml = indexHtmlBuilder.html(request, initialStateProvider);
        response.body(indexHtml);
        response.header(RESPONSE_HEADER_CONTENT_TYPE, "text/html; charset=utf-8");
        if (responseModifier != null) {
            responseModifier.modify(request, response);
        }
    }

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
        this.html(classPathLocation, fileSystemLocation, null);
    }

    protected void html(String classPathLocation, String fileSystemLocation, InitialStateProvider initialStateProvider) {
        this.html(classPathLocation, fileSystemLocation, initialStateProvider, null);
    }

    protected void html(String classPathLocation, String fileSystemLocation, InitialStateProvider initialStateProvider,
            HtmlResponseModifier responseModifier) {
        this.html(classPathLocation, fileSystemLocation, initialStateProvider, responseModifier, TRANSFORMER);
    }

    protected void html(String classPathLocation, String fileSystemLocation, InitialStateProvider initialStateProvider,
            HtmlResponseModifier responseModifier, HtmlEscapingAwareJsonTransformer transformer) {
        boolean isDev = ProfileDetector.isDev(this.environment);
        IndexHtmlBuilder indexHtmlBuilder = new IndexHtmlBuilder(classPathLocation, fileSystemLocation, transformer, isDev);
        StaticFilesConfiguration staticHandler = createStaticHandler(classPathLocation, fileSystemLocation, isDev);

        Spark.after((request, response) -> {
            // always set expiry headers
            setExpiryHeaders(request, response);

            // if some Spark resource has already produced a result, stop here
            if (!"GET".equalsIgnoreCase(request.requestMethod()) || StringUtils.isNotEmpty(response.body())) {
                return;
            }

            String pathInfo = request.pathInfo();

            // request to the root path
            if ("/".equals(pathInfo)) {
                sendIndexHtml(request, response, indexHtmlBuilder, initialStateProvider, responseModifier);
                return;
            }

            // look for static resources
            boolean staticResourceSent = staticHandler.consume(request.raw(), response.raw());

            // otherwise send the index.html
            if (!staticResourceSent) {
                sendIndexHtml(request, response, indexHtmlBuilder, initialStateProvider, responseModifier);
            }
        });
    }
}
