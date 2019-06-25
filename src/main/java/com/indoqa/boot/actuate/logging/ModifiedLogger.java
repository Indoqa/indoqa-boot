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

import java.time.Duration;
import java.time.Instant;

public class ModifiedLogger {

    private String logger;
    private String originalLevel;
    private String currentLevel;
    private long executionTime;
    private Duration duration;
    private String modificationKey;

    public static ModifiedLogger of(String originalLevel, String currentLevel, long executionTime, Duration duration,
        String modificationKey) {
        return of(null, originalLevel, currentLevel, executionTime, duration, modificationKey);
    }

    public static ModifiedLogger of(String logger, String originalLevel, String currentLevel, long executionTime, Duration duration,
        String modificationKey) {
        ModifiedLogger result = new ModifiedLogger();

        result.setLogger(logger);
        result.setOriginalLevel(originalLevel);
        result.setCurrentLevel(currentLevel);
        result.setExecutionTime(executionTime);
        result.setDuration(duration);
        result.setModificationKey(modificationKey);

        return result;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public String getOriginalLevel() {
        return originalLevel;
    }

    public void setOriginalLevel(String originalLevel) {
        this.originalLevel = originalLevel;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public String getModificationKey() {
        return modificationKey;
    }

    public void setModificationKey(String modificationKey) {
        this.modificationKey = modificationKey;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Logger '");
        stringBuilder.append(logger);
        stringBuilder.append("' set to '");
        stringBuilder.append(currentLevel);
        stringBuilder.append("' until ");
        stringBuilder.append(Instant.ofEpochMilli(executionTime));
        stringBuilder.append(" (in ");
        stringBuilder.append(duration);
        stringBuilder.append(")\n\t");
        stringBuilder.append("Modification-Key: ");
        stringBuilder.append(modificationKey);
        return stringBuilder.toString();
    }
}
