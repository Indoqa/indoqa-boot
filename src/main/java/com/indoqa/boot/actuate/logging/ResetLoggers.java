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
package com.indoqa.boot.actuate.logging;

import static com.indoqa.boot.actuate.logging.ResetStatus.*;

import java.util.HashMap;
import java.util.Map;

public class ResetLoggers {

    private final ResetStatus status;
    private Map<String, String> reset;

    public ResetLoggers(ResetStatus status) {
        this.status = status;
    }

    public static ResetLoggers nonReset() {
        ResetLoggers result = new ResetLoggers(NON_RESET);
        return result;
    }

    public static ResetLoggers reset() {
        ResetLoggers result = new ResetLoggers(RESET);
        result.setReset(new HashMap<>());
        return result;
    }

    public ResetStatus getStatus() {
        return status;
    }

    public Map<String, String> getReset() {
        return reset;
    }

    public void setReset(Map<String, String> reset) {
        this.reset = reset;
    }

    public void add(String logger, String level) {
        this.reset.put(logger, level);
    }

    @Override
    public String toString() {
        if (NON_RESET.equals(status)) {
            return "No loggers to reset.";
        }

        StringBuilder responseBuilder = new StringBuilder();
        if (ResetStatus.RESET.equals(status)) {
            responseBuilder.append("Reset the following loggers:\n");
            this.reset.entrySet().forEach(entry -> {
                responseBuilder.append(entry.getKey());
                responseBuilder.append(" to ");
                responseBuilder.append(entry.getValue());
                responseBuilder.append("\n");
            });
        }

        return responseBuilder.toString();
    }
}
