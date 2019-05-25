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
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.html.builder.HtmlBuilder;
import com.indoqa.boot.html.react.AbstractCreateReactAppResourceBase.ResponseEnhancements;
import com.indoqa.boot.json.transformer.HtmlEscapingAwareJsonTransformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import spark.Request;

public class IndexHtmlBuilder {

    private static final Logger LOGGER = getLogger(IndexHtmlBuilder.class);
    private static final String EMPTY_INITIAL_STATE = "{}";

    private final String classPathLocation;
    private final String fileSystemLocation;
    private final boolean isDev;

    private HtmlParts htmlParts = null;

    IndexHtmlBuilder(String classPathLocation, String fileSystemLocation, boolean isDev) {
        this.classPathLocation = classPathLocation;
        this.fileSystemLocation = fileSystemLocation;
        this.isDev = isDev;
        parseIndexHtml(classPathLocation, fileSystemLocation);
    }

    private static String appendInitialStateScript(Request request, ResponseEnhancements responseEnhancements) {
        String initialStateJson = EMPTY_INITIAL_STATE;
        if (responseEnhancements != null) {
            InitialStateProvider initialStateProvider = responseEnhancements.getInitialStateProvider();
            HtmlEscapingAwareJsonTransformer jsonTransformer = responseEnhancements.getJsonTransformer();
            if (initialStateProvider != null && jsonTransformer != null) {
                Object initialStateObject = initialStateProvider.initialState(request);
                if (initialStateObject != null) {
                    initialStateJson = jsonTransformer.render(initialStateObject);
                }
            }
        }
        return new StringBuilder()
            .append("<script>window.__INITIAL_STATE__ = ")
            .append(initialStateJson)
            .append(";</script>")
            .toString();
    }

    private static String appendAdditionalHeadContent(Request request, ResponseEnhancements responseEnhancements) {
        if (responseEnhancements == null) {
            return EMPTY;
        }

        HtmlBuilder headerBuilder = responseEnhancements.getHeaderBuilder();
        if (headerBuilder == null) {
            return EMPTY;
        }
        return headerBuilder.html(request);
    }

    private static String appendTitle(String staticTitle, ResponseEnhancements responseEnhancements) {
        if (responseEnhancements == null) {
            return staticTitle;
        }

        String title = responseEnhancements.getTitle();
        if (title != null) {
            return "<title>" + title + "</title>";
        }
        return staticTitle;
    }

    public String html(Request request, ResponseEnhancements responseEnhancements) {
        if (this.isDev) {
            parseIndexHtml(this.classPathLocation, this.fileSystemLocation);
        }
        return new StringBuilder()
            .append(this.htmlParts.getBeforeHeadContent())
            .append(this.htmlParts.getHeadElement())
            .append(this.htmlParts.getHeadContent())
            .append(appendAdditionalHeadContent(request, responseEnhancements))
            .append(appendTitle(this.htmlParts.getTitle(), responseEnhancements))
            .append("</head>")
            .append(this.htmlParts.getBodyElement())
            .append(appendInitialStateScript(request, responseEnhancements))
            .append(this.htmlParts.getBodyContent())
            .append("</body>")
            .append("</html>")
            .toString();
    }

    private void parseIndexHtml(String localClassPathLocation, String localFileSystemLocation) {
        this.htmlParts = new HtmlParts(readIndexHtml(localClassPathLocation, localFileSystemLocation));
    }

    private String readIndexHtml(String localClassPathLocation, String localFileSystemLocation) {
        if (this.isDev) {
            File filesystemIndexHtml = new File(localFileSystemLocation, "index.html");

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
        String classpathIndexHtml = localClassPathLocation + "/index.html";
        try {
            return IOUtils.toString(this.getClass().getResourceAsStream(classpathIndexHtml), UTF_8);
        } catch (IOException e) {
            throw new ApplicationInitializationException("Error while reading index.html from classpath: " + classpathIndexHtml);
        }
    }

    static class HtmlParts {

        private static final Pattern HEAD_PATTERN = Pattern.compile(
            "((<head(?:\\s+[a-z]+(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)))*\\s*>)([\\S\\s]*)</head>)");
        private static final Pattern BODY_PATTERN = Pattern.compile(
            "((<body(?:\\s+[a-z]+(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)))*\\s*>)([\\S\\s]*)</body>)");
        private static final Pattern TITLE_PATTERN = Pattern.compile(
            "((<title(?:\\s+[a-z]+(?:\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)))*\\s*>)([\\S\\s]*)</title>)");

        private String beforeHeadContent = "<!DOCTYPE html>\n<html lang=\"de\">";
        private String headContent;
        private String headElement = "<head>";
        private String title;
        private String bodyElement = "<body>";
        private String bodyContent;

        HtmlParts(String html) {
            if (StringUtils.isBlank(html)) {
                return;
            }
            Matcher headerMatcher = HEAD_PATTERN.matcher(html);
            if (headerMatcher.find()) {
                this.headElement = headerMatcher.group(2);
                String headContent = headerMatcher.group(3);
                Matcher titleMatcher = TITLE_PATTERN.matcher(headContent);
                if (titleMatcher.find()) {
                    this.title = titleMatcher.group(0);
                }
                this.headContent = titleMatcher.replaceFirst("");
            }
            Matcher bodyMatcher = BODY_PATTERN.matcher(html);
            if (bodyMatcher.find()) {
                this.bodyElement = bodyMatcher.group(2);
                this.bodyContent = bodyMatcher.group(3);
            }
            int posHead = html.indexOf("<head");
            if (posHead > 0) {
                this.beforeHeadContent = html.substring(0, posHead);
            }
        }

        String getBeforeHeadContent() {
            return this.beforeHeadContent;
        }

        String getTitle() {
            return this.title;
        }

        String getHeadElement() {
            return this.headElement;
        }

        String getHeadContent() {
            return this.headContent;
        }

        String getBodyElement() {
            return this.bodyElement;
        }

        String getBodyContent() {
            return this.bodyContent;
        }
    }
}
