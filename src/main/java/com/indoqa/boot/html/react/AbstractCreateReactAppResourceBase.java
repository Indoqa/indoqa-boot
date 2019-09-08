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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

import com.indoqa.boot.html.builder.HtmlBuilder;
import com.indoqa.boot.html.react.AbstractCreateReactAppResourceBase.ResponseEnhancements.ResponseEnhancementsBuilder;
import com.indoqa.boot.html.resources.AbstractHtmlResourcesBase;
import com.indoqa.boot.html.resources.HtmlResponseModifier;
import com.indoqa.boot.json.transformer.HtmlEscapingAwareJsonTransformer;
import com.indoqa.boot.json.transformer.HtmlEscapingJacksonTransformer;
import com.indoqa.boot.profile.ProfileDetector;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

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

    private static final ResponseEnhancementsProvider DEFAULT_REQUEST_ENHANCEMENTS_PROVIDER = (req, res) -> {
        return new ResponseEnhancementsBuilder().build();
    };

    @Inject
    private Environment environment;

    private static StaticFilesConfiguration createStaticHandler(String classPathLocation, String fileSystemLocation, boolean isDev) {
        StaticFilesConfiguration staticHandler = new StaticFilesConfiguration();
        if (isDev) {
            staticHandler.configureExternal(fileSystemLocation);
        }
        else {
            staticHandler.configure(classPathLocation);
        }
        return staticHandler;
    }

    private static void sendIndexHtml(Request request, Response response, IndexHtmlBuilder indexHtmlBuilder,
        ResponseEnhancements responseEnhancements) {
        response.body(indexHtmlBuilder.html(request, responseEnhancements));
        response.header(RESPONSE_HEADER_CONTENT_TYPE, "text/html; charset=utf-8");
        if (responseEnhancements == null) {
            return;
        }
        HtmlResponseModifier htmlResponseModifier = responseEnhancements.getHtmlResponseModifier();
        if (htmlResponseModifier != null) {
            htmlResponseModifier.modify(request, response);
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
        this.html(classPathLocation, fileSystemLocation, DEFAULT_REQUEST_ENHANCEMENTS_PROVIDER);
    }

    protected void html(String classPathLocation, String fileSystemLocation,
        ResponseEnhancementsProvider responseEnhancementsProvider) {
        Objects.requireNonNull(responseEnhancementsProvider, "ResponseEnhancementsProvider must not be null.");

        ReactApplications reactApplications = new ReactApplications();
        reactApplications.add(classPathLocation, fileSystemLocation, responseEnhancementsProvider, (req, res) -> true);
        this.html(reactApplications);
    }

    protected void html(ReactApplications reactApplications) {
        Objects.requireNonNull(reactApplications, "ReactApplications must not be null.");
        Spark.after((request, response) -> {
            ReactApplication reactApplication = reactApplications.lookup(request, response);
            if (reactApplication == null) {
                Spark.notFound("No React application available.");
                return;
            }

            // always set expiry headers
            setExpiryHeaders(request, response);

            // if some Spark resource has already produced a result, stop here
            if (!"GET".equalsIgnoreCase(request.requestMethod()) || StringUtils.isNotEmpty(response.body())) {
                return;
            }

            String pathInfo = request.pathInfo();
            ResponseEnhancements responseEnhancements = reactApplication.getResponseEnhancementsProvider().enhance(request, response);

            // request to the root path
            if ("/".equals(pathInfo)) {
                sendIndexHtml(request, response, reactApplication.getIndexHtmlBuilder(), responseEnhancements);
                return;
            }

            // look for static resources
            boolean staticResourceSent = reactApplication.getStaticFilesConfiguration().consume(request.raw(), response.raw());

            // otherwise send the index.html
            if (!staticResourceSent) {
                sendIndexHtml(request, response, reactApplication.getIndexHtmlBuilder(), responseEnhancements);
            }
        });
    }

    @FunctionalInterface
    public interface ResponseEnhancementsProvider {

        ResponseEnhancements enhance(Request req, Response res);
    }

    @FunctionalInterface
    public interface TestReactApplication {

        boolean invoke(Request req, Response res);
    }

    private class ReactApplication {

        private final StaticFilesConfiguration staticFilesConfiguration;
        private final IndexHtmlBuilder indexHtmlBuilder;
        private final ResponseEnhancementsProvider responseEnhancementsProvider;
        private final TestReactApplication testReactApplication;

        ReactApplication(StaticFilesConfiguration conf, IndexHtmlBuilder indexHtmlBuilder, ResponseEnhancementsProvider provider,
            TestReactApplication testReactApplication) {
            this.staticFilesConfiguration = conf;
            this.indexHtmlBuilder = indexHtmlBuilder;
            this.responseEnhancementsProvider = provider;
            this.testReactApplication = testReactApplication;
        }

        ResponseEnhancementsProvider getResponseEnhancementsProvider() {
            return this.responseEnhancementsProvider;
        }

        StaticFilesConfiguration getStaticFilesConfiguration() {
            return this.staticFilesConfiguration;
        }

        IndexHtmlBuilder getIndexHtmlBuilder() {
            return this.indexHtmlBuilder;
        }
    }

    public class ReactApplications {

        private final List<ReactApplication> apps = new ArrayList<>();

        public void add(String classPathLocation, String fileSystemLocation, ResponseEnhancementsProvider responseEnhancementsProvider,
            TestReactApplication testReactApplication) {
            boolean isDev = ProfileDetector.isDev(AbstractCreateReactAppResourceBase.this.environment);
            StaticFilesConfiguration staticHandler = createStaticHandler(classPathLocation, fileSystemLocation, isDev);
            IndexHtmlBuilder indexHtmlBuilder = new IndexHtmlBuilder(classPathLocation, fileSystemLocation, isDev);
            this.apps.add(new ReactApplication(staticHandler, indexHtmlBuilder, responseEnhancementsProvider, testReactApplication));
        }

        ReactApplication lookup(Request req, Response res) {
            for (ReactApplication app : this.apps) {
                if (app.testReactApplication.invoke(req, res)) {
                    return app;
                }
            }
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class ResponseEnhancements {

        private static final HtmlEscapingAwareJsonTransformer JSON_TRANSFORMER = new HtmlEscapingJacksonTransformer();

        private final String title;
        private final String lang;
        private final InitialStateProvider initialStateProvider;
        private final HtmlBuilder headerBuilder;
        private final HtmlResponseModifier htmlResponseModifier;
        private final HtmlEscapingAwareJsonTransformer jsonTransformer;

        ResponseEnhancements(ResponseEnhancementsBuilder builder) {
            this.title = builder.title;
            this.lang = builder.lang;
            this.initialStateProvider = builder.initialStateProvider;
            this.headerBuilder = builder.headerBuilder;
            this.htmlResponseModifier = builder.htmlResponseModifier;
            this.jsonTransformer = builder.jsonTransformer;
        }

        HtmlEscapingAwareJsonTransformer getJsonTransformer() {
            return this.jsonTransformer;
        }

        HtmlResponseModifier getHtmlResponseModifier() {
            return this.htmlResponseModifier;
        }

        String getTitle() {
            return this.title;
        }

        String getLang() {
            return this.lang;
        }

        InitialStateProvider getInitialStateProvider() {
            return this.initialStateProvider;
        }

        HtmlBuilder getHeaderBuilder() {
            return this.headerBuilder;
        }

        @SuppressWarnings("unused")
        public static class ResponseEnhancementsBuilder {

            private String title;
            private String lang;
            private InitialStateProvider initialStateProvider;
            private HtmlBuilder headerBuilder;
            private HtmlResponseModifier htmlResponseModifier;
            private HtmlEscapingAwareJsonTransformer jsonTransformer = ResponseEnhancements.JSON_TRANSFORMER;

            public ResponseEnhancementsBuilder setTitle(String title) {
                this.title = title;
                return this;
            }

            public ResponseEnhancementsBuilder setLang(String lang) {
                this.lang = lang;
                return this;
            }

            public ResponseEnhancementsBuilder setInitialStateProvider(InitialStateProvider initialStateProvider) {
                this.initialStateProvider = initialStateProvider;
                return this;
            }

            public ResponseEnhancementsBuilder setHeaderBuilder(HtmlBuilder headerBuilder) {
                this.headerBuilder = headerBuilder;
                return this;
            }

            public ResponseEnhancementsBuilder setHtmlResponseModifier(HtmlResponseModifier htmlResponseModifier) {
                this.htmlResponseModifier = htmlResponseModifier;
                return this;
            }

            public ResponseEnhancementsBuilder setJsonTransformer(HtmlEscapingAwareJsonTransformer jsonTransformer) {
                this.jsonTransformer = jsonTransformer;
                return this;
            }

            public ResponseEnhancements build() {
                return new ResponseEnhancements(this);
            }
        }
    }
}
