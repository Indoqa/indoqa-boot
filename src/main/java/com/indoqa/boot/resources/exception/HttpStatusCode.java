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
package com.indoqa.boot.resources.exception;

public enum HttpStatusCode {

    ACCEPTED(202, true), OK(200, true), NO_CONTENT(204, false), NOT_MODIFIED(304, false), BAD_REQUEST(400, true), FORBIDDEN(403, true),
    NOT_FOUND(404, true), INTERNAL_SERVER_ERROR(500, true);

    private final int code;
    private final boolean hasBody;

    private HttpStatusCode(int code, boolean hasBody) {
        this.code = code;
        this.hasBody = hasBody;
    }

    public int getCode() {
        return this.code;
    }

    public boolean hasBody() {
        return this.hasBody;
    }

    public boolean matches(int statusCode) {
        return this.code == statusCode;
    }

    public boolean notMatches(int statusCode) {
        return !this.matches(statusCode);
    }
}
