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

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.indoqa.boot.html.HtmlBuilder;
import com.indoqa.boot.json.HtmlEscapingAwareJsonTransformer;

import spark.Request;

public class ReactHtmlBuilder implements HtmlBuilder {

    private static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    private static final String EMPTY_INITIAL_STATE = "{}";

    private String mainCssPath;
    private String mainJavascriptPath;
    private String rootElementId = "app";

    private InitialStateProvider initialStateProvider = req -> null;
    private HtmlEscapingAwareJsonTransformer jsonTransformer;
    private Map<String, String> proxyUrlMappings = new HashMap<>();

    private List<String> headHtml = new ArrayList<>();
    private List<String> postAppHtml = new ArrayList<>();
    private List<String> preAppHtml = new ArrayList<>();

    public ReactHtmlBuilder() {
        this.initialStateProvider = req -> null;
    }

    public ReactHtmlBuilder addHeadHtml(String html) {
        this.headHtml.add(html);
        return this;
    }

    public ReactHtmlBuilder addPostAppHtml(String html) {
        this.postAppHtml.add(html);
        return this;
    }

    public ReactHtmlBuilder addPreAppHtml(String html) {
        this.preAppHtml.add(html);
        return this;
    }

    @Override
    public String html(Request request) {
        return new StringBuilder()
            .append("<!DOCTYPE html><html><head>")
            .append("<meta http-equiv=\"")
            .append(RESPONSE_HEADER_CONTENT_TYPE)
            .append("\" content=\"")
            .append(CONTENT_TYPE_HTML)
            .append("\">")
            .append("<link rel=\"stylesheet\" href=\"")
            .append(this.mainCssPath)
            .append("\" />")
            .append(this.headHtml.stream().collect(joining(" ")))
            .append("</head>")
            .append("<body>")
            .append(this.preAppHtml.stream().collect(joining(" ")))
            .append("<div id=\"")
            .append(this.rootElementId)
            .append("\"></div>")
            .append(this.postAppHtml.stream().collect(joining(" ")))
            .append("<script>window.__INITIAL_STATE__ = ")
            .append(this.createInitialStateJson(request))
            .append(";</script>")
            .append("<script>")
            .append(this.createProxyMappingScript())
            .append(";</script>")
            .append("<script src=\"")
            .append(this.mainJavascriptPath)
            .append("\"></script>")
            .append("</body></html>")
            .toString();
    }

    public ReactHtmlBuilder initialStateProvider(InitialStateProvider initialStateProvider,
            HtmlEscapingAwareJsonTransformer jsonTransformer) {
        this.initialStateProvider = initialStateProvider;
        this.jsonTransformer = jsonTransformer;
        return this;
    }

    public ReactHtmlBuilder mainCssPath(String mainCssPath) {
        this.mainCssPath = mainCssPath;
        return this;
    }

    public ReactHtmlBuilder mainJavascriptPath(String mainJavascriptPath) {
        this.mainJavascriptPath = mainJavascriptPath;
        return this;
    }

    public ReactHtmlBuilder rootElementId(String rootElementId) {
        this.rootElementId = rootElementId;
        return this;
    }

    private String createInitialStateJson(Request request) {
        String initialStateJson = EMPTY_INITIAL_STATE;
        if (this.initialStateProvider != null && this.jsonTransformer != null) {
            Object initialStateObject = this.initialStateProvider.initialState(request);

            if (initialStateObject != null) {
                initialStateJson = this.jsonTransformer.render(initialStateObject);
            }
        }
        return initialStateJson;
    }

    private String createProxyMappingEntryScript(Entry<String, String> entry) {
        return new StringBuilder()
            .append("window.")
            .append(entry.getKey())
            .append(" = ")
            .append("'")
            .append(entry.getValue())
            .append("'")
            .append(";")
            .toString();
    }

    private String createProxyMappingScript() {
        if (this.proxyUrlMappings == null) {
            return "";
        }

        return this.proxyUrlMappings.entrySet().stream().map(this::createProxyMappingEntryScript).collect(joining("\n"));
    }
}
