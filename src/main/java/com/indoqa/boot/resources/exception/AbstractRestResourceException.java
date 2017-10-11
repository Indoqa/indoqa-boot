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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for exceptions in resources. Extend it to make use of descriptive errors for the consumers of resources.<br>
 * It is possible to transport error properties to the consumer for e.g. i18n.
 */
public abstract class AbstractRestResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatusCode statusCode;
    private final ErrorData errorData;

    public AbstractRestResourceException(String message, HttpStatusCode statusCode, String type) {
        super(message);

        this.statusCode = statusCode;
        this.errorData = ErrorData.create(type, message);
    }

    public AbstractRestResourceException(String message, HttpStatusCode statusCode, String type, Throwable cause) {
        super(message, cause);

        this.statusCode = statusCode;
        this.errorData = ErrorData.create(type, message);
    }

    public ErrorData getErrorData() {
        return this.errorData;
    }

    public HttpStatusCode getStatusCode() {
        return this.statusCode;
    }

    public void setParameter(String name, Object value) {
        this.errorData.setParameter(name, value);
    }

    public static class ErrorData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String type;
        private String message;
        // Sonar flags this as "not serializable", but it is, since HashMap is serializable
        private final Map<String, Object> parameters = new HashMap<>(); // NOSONAR

        public static ErrorData create(String type, String message) {
            ErrorData result = new ErrorData();

            result.setType(type);
            result.setMessage(message);

            return result;
        }

        public String getMessage() {
            return this.message;
        }

        public Map<String, Object> getParameters() {
            return this.parameters;
        }

        public String getType() {
            return this.type;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setParameter(String name, Object value) {
            this.parameters.put(name, value);
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
