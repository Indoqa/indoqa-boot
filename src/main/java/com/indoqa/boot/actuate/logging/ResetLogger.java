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

public class ResetLogger {

    private ResetStatus status;
    private String logger;
    private String level;

    public static ResetLogger notReset(String logger) {
        ResetLogger result = new ResetLogger();
        result.setStatus(ResetStatus.NON_RESET);
        result.setLogger(logger);
        return result;
    }

    public static ResetLogger reset(String logger, String level) {
        ResetLogger result = new ResetLogger();

        result.setStatus(ResetStatus.RESET);
        result.setLogger(logger);
        result.setLevel(level);

        return result;
    }

    public ResetStatus getStatus() {
        return status;
    }

    public void setStatus(ResetStatus status) {
        this.status = status;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @Override
    public String toString() {
        if (ResetStatus.NON_RESET.equals(status)) {
            return "Cannot reset logger '" + logger + "'. Logger is not modified.";
        }

        return "Reset logger: '" + logger + "' to " + level;
    }
}
