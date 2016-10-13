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

import static java.util.stream.Collectors.joining;

import java.util.Map.Entry;

import com.indoqa.boot.json.JsonTransformer;

import spark.Request;
import spark.Spark;

/*default*/ final class JsAppUtils {

    private static final String ACCEPT_TYPE_TEXT_HTML = "text/html";
    private static final String EMPTY_INITIAL_STATE = "{}";
    private static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";

    private JsAppUtils() {
        // hide utility class constructor
    }

    public static void jsApp(String path, Assets assets, ProxyURLMappings urlMappings, InitialStateProvider initialState,
            JsonTransformer transformer) {
        Spark.get(path, ACCEPT_TYPE_TEXT_HTML, (req, res) -> {
            String initialStateJson = createInitialStateJson(req, initialState, transformer);
            String proxyMappingScript = createProxyMappingScript(urlMappings);

            res.raw().setContentType(CONTENT_TYPE_HTML);

            return createSinglePageHtml(assets, proxyMappingScript, initialStateJson);
        });
    }

    protected static String createInitialStateJson(Request req, InitialStateProvider initialStateProvider,
            JsonTransformer transformer) {
        String initialStateJson = EMPTY_INITIAL_STATE;
        if (initialStateProvider != null && transformer != null) {
            Object initialStateObject = initialStateProvider.initialState(req);
            if (initialStateObject != null) {
                initialStateJson = transformer.render(initialStateObject);
            }
        }
        return initialStateJson;
    }

    protected static String createProxyMappingEntryScript(Entry<String, String> entry) {
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

    protected static String createProxyMappingScript(ProxyURLMappings urlMappings) {
        if (urlMappings == null) {
            return "";
        }

        return urlMappings.getEntries().stream().map(JsAppUtils::createProxyMappingEntryScript).collect(joining("\n"));
    }

    private static String createSinglePageHtml(Assets assets, String proxyMappingScript, String initialStateJson) {
        return new StringBuilder()
            .append("<!DOCTYPE html><html><head>")
            .append("<meta http-equiv=\"")
            .append(RESPONSE_HEADER_CONTENT_TYPE)
            .append("\" content=\"")
            .append(CONTENT_TYPE_HTML)
            .append("\">")
            .append("<link rel=\"stylesheet\" href=\"")
            .append(assets.getMainCss())
            .append("\" />")
            .append("</head>")
            .append("<body>")
            .append("<div id=\"")
            .append(assets.getRootElementId())
            .append("\"></div>")
            .append("<script>window.__INITIAL_STATE__ = ")
            .append(initialStateJson)
            .append(";</script>")
            .append("<script>")
            .append(proxyMappingScript)
            .append(";</script>")
            .append("<script src=\"")
            .append(assets.getMainJavascript())
            .append("\"></script>")
            .append("</body></html>")
            .toString();
    }
}
