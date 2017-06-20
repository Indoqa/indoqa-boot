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
package com.indoqa.boot.html.resources;

import java.util.Collections;
import java.util.Set;

import com.indoqa.boot.html.builder.HtmlBuilder;

import spark.Filter;
import spark.Spark;
import spark.utils.MimeParse;

public abstract class AbstractHtmlResourcesBase {

    private static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    private static final Set<String> ACCEPTED_TYPES = Collections.singleton("text/html");

    protected void html(String path, HtmlBuilder htmlBuilder) {
        Spark.after(path, (Filter) (req, res) -> {
            if (res.body() != null) {
                return;
            }

            String acceptHeader = req.headers("Accept");
            if (acceptHeader == null) {
                return;
            }

            String bestMatch = MimeParse.bestMatch(ACCEPTED_TYPES, acceptHeader);
            if (bestMatch.equals(MimeParse.NO_MIME_TYPE)) {
                return;
            }

            res.type(CONTENT_TYPE_HTML);
            res.body(htmlBuilder.html(req));
        });
    }
}
