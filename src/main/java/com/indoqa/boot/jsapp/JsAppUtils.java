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

import com.indoqa.boot.json.JsonTransformer;

import spark.Request;
import spark.Spark;

public final class JsAppUtils {

    private static final String EMPTY_INITIAL_STATE = "{}";
    private static final String RESPONSE_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";

    private JsAppUtils() {
        // hide utility class constructor
    }

    public static void jsApp(String path, Assets assets) {
        jsApp(path, assets, null, null);
    }

    public static void jsApp(String path, Assets assets, InitialStateProvider initialState, JsonTransformer transformer) {
        Spark.get(path, (req, res) -> {
            String initialStateJson = createInitialStateJson(req, initialState, transformer);
            res.header(RESPONSE_HEADER_CONTENT_TYPE, CONTENT_TYPE_HTML);
            return createSinglePageHtml(assets.getRootElementId(), assets.getMainCss(), assets.getMainJavascript(), initialStateJson);
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

    private static String createSinglePageHtml(String rootElementId, String cssFile, String javascriptFile, String initialStateJson) {
        return new StringBuilder()
            .append("<!DOCTYPE html><html><head>")
            .append("<meta http-equiv=\"")
            .append(RESPONSE_HEADER_CONTENT_TYPE)
            .append("\" content=\"")
            .append(CONTENT_TYPE_HTML)
            .append("\">")
            .append("<link rel=\"stylesheet\" href=\"")
            .append(cssFile)
            .append("\" />")
            .append("</head>")
            .append("<body>")
            .append("<div id=\"")
            .append(rootElementId)
            .append("\"></div>")
            .append("<script>window.__INITIAL_STATE__ = ")
            .append(initialStateJson)
            .append(";</script>")
            .append("<script src=\"")
            .append(javascriptFile)
            .append("\"></script>")
            .append("</body></html>")
            .toString();
    }
}
