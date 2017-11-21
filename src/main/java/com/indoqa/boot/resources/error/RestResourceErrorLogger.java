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
package com.indoqa.boot.resources.error;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import spark.Request;

/*default*/ final class RestResourceErrorLogger {

    private static final Logger LOGGER = getLogger(RestResourceErrorLogger.class);
    private static final String UNKNOWN = "unknown";

    private RestResourceErrorLogger() {
        // hide utility class constructor
    }

    static void logException(Request req, RestResourceError error, Exception e) {
        int status = error.getStatus();
        String errorMessage = getErrorMessage(error);
        String requestMessage = getRequestMessage(req);
        switch (status / 100) {
            case 3:
            case 4:
                LOGGER.info("{} {} :{} line: {}", requestMessage, errorMessage, getRootCauseMessage(e), getExceptionLocation(e));
                LOGGER.debug("ClientErrorUUID: {}: Exception: ", error.getId(), e);
                break;
            case 2:
                LOGGER.warn("Unexpected log of exception {} ({}), ", requestMessage, errorMessage, e);
                break;
            default:
                LOGGER.error("Error executing {}. {}.", requestMessage, errorMessage, e);
        }
    }

    private static String getErrorMessage(RestResourceError resourceError) {
        return new StringBuilder()
            .append("Client error: ")
            .append(resourceError.getStatus())
            .append(": '")
            .append(resourceError.getError())
            .append("' UUID: {{{ ")
            .append(resourceError.getId())
            .append(" }}}")
            .toString();
    }

    private static String getExceptionLocation(Exception e) {
        Throwable rootCause = getRootCause(e);

        Throwable cause = ofNullable(rootCause).orElse(e);
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return UNKNOWN;
        }

        StackTraceElement element = stackTrace[0];
        return element.getFileName() + ":" + element.getLineNumber();
    }

    private static String getRequestMessage(Request req) {
        return new StringBuilder().append(req.requestMethod()).append(" @ '").append(req.uri()).append('\'').toString();
    }
}
