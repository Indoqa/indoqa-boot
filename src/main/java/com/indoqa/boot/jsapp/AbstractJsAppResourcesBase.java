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

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.indoqa.boot.json.HtmlEscapingAwareJsonTransformer;
import com.indoqa.boot.json.JsonTransformer;

public abstract class AbstractJsAppResourcesBase {

    private static final Logger LOGGER = getLogger(AbstractJsAppResourcesBase.class);

    @Inject
    private JsonTransformer jsonTransformer;

    @PostConstruct
    public void checkTransformer() {
        if (!(this.jsonTransformer instanceof HtmlEscapingAwareJsonTransformer)) {
            String url = "https://medium.com/node-security/the-most-common-xss-vulnerability-in-react-js-applications-2bdffbcc1fa0#.xf5lxr3zz";
            LOGGER.warn(
                "This application does not use a transformer that implements the {} interface which is used to mark a "
                    + "JSON transformer that escapes HTML/XML syntax to protect against XSS attacks if an initial state "
                    + "is provided. See {} for details and protect your application.",
                HtmlEscapingAwareJsonTransformer.class.getName(), url);
        }
    }

    public void jsApp(String path, Assets assets) {
        JsAppUtils.jsApp(path, assets, null, null, null);
    }

    public void jsApp(String path, Assets assets, InitialStateProvider initialState) {
        JsAppUtils.jsApp(path, assets, null, initialState, this.jsonTransformer);
    }

    public void jsApp(String path, Assets assets, ProxyURLMappings urlMappings) {
        JsAppUtils.jsApp(path, assets, urlMappings, null, null);
    }

    public void jsApp(String path, Assets assets, ProxyURLMappings urlMappings, InitialStateProvider initialState) {
        JsAppUtils.jsApp(path, assets, urlMappings, initialState, this.jsonTransformer);
    }
}
