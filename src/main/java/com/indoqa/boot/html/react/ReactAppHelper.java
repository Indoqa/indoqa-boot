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

import static java.lang.Boolean.TRUE;

import org.apache.commons.lang3.StringUtils;

import spark.Request;

public final class ReactAppHelper {

    private static final String ATTRIBUTE_IGNORE = "com.indoqa.boot.IGNORE";

    private ReactAppHelper() {
        // utility class
    }

    public static void ignoreRequest(Request request) {
        request.attribute(ATTRIBUTE_IGNORE, TRUE);
    }

    public static boolean shouldIgnoreRequest(Request request) {
        boolean unsupportedRequestMethod = isUnSupportedRequestMethod(request);
        boolean websocketUpgradeRequest = isWebsocketUpgradeRequest(request);
        boolean explicitlyIgnoredRequest = TRUE.equals(request.attribute(ATTRIBUTE_IGNORE));
        return unsupportedRequestMethod || websocketUpgradeRequest || explicitlyIgnoredRequest;
    }

    private static boolean isWebsocketUpgradeRequest(Request request) {
        return StringUtils.equalsIgnoreCase(request.headers("Upgrade"), "websocket");
    }

    private static boolean isUnSupportedRequestMethod(Request request) {
        switch (request.requestMethod()) {
            case "GET":
            case "HEAD":
                return false;
            default:
                return true;
        }
    }
}
