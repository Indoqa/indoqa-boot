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

public class LogLevelResponse {

    private String logger;
    private String level;
    private final LoggerStatus loggerStatus;

    public LogLevelResponse(LoggerStatus loggerStatus) {
        this.loggerStatus = loggerStatus;
    }

    public static LogLevelResponse modified(String logger, String level) {
        LogLevelResponse result = new LogLevelResponse(LoggerStatus.MODIFIED);
        result.setLogger(logger);
        result.setLevel(level);
        return result;
    }

    public static LogLevelResponse original(String logger, String level) {
        LogLevelResponse result = new LogLevelResponse(LoggerStatus.ORIGINAL);
        result.setLogger(logger);
        result.setLevel(level);
        return result;
    }

    public static LogLevelResponse notExising(String logger) {
        LogLevelResponse result = new LogLevelResponse(LoggerStatus.NON_EXISTING);
        result.setLogger(logger);
        return result;
    }

    public LoggerStatus getLoggerStatus() {
        return loggerStatus;
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
        if (LoggerStatus.NON_EXISTING.equals(loggerStatus)) {
            return logger + ": does not exist.";
        }
        if (LoggerStatus.MODIFIED.equals(loggerStatus)) {
            return logger + " : modified to " + level;
        }
        return logger + " : " + level;
    }
}
