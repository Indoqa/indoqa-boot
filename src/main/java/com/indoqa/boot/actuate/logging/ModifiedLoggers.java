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

import static com.indoqa.boot.actuate.logging.ModificationStatus.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ModifiedLoggers {

    private final ModificationStatus status;
    private Map<String, ModifiedLogger> modified;

    public ModifiedLoggers(ModificationStatus status) {
        this.status = status;
    }

    public static ModifiedLoggers nonModified() {
        return new ModifiedLoggers(NON_MODIFIED);
    }

    public static ModifiedLoggers modified() {
        ModifiedLoggers result = new ModifiedLoggers(MODIFIED);

        result.setModified(new HashMap<>());

        return result;
    }

    public ModificationStatus getStatus() {
        return status;
    }

    public void add(String loggerName, String originalLevel, String currentLevel, long executionTime, Duration duration,
        String modificationKey) {
        this.modified.put(loggerName, ModifiedLogger.of(originalLevel, currentLevel, executionTime, duration, modificationKey));
    }

    public Map<String, ModifiedLogger> getModified() {
        return modified;
    }

    public void setModified(Map<String, ModifiedLogger> modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        if (NON_MODIFIED.equals(status)) {
            return "No modified loggers.";
        }

        StringBuilder responseBuilder = new StringBuilder();

        this.modified.entrySet().forEach(entry -> {
            ModifiedLogger value = entry.getValue();
            responseBuilder.append(entry.getKey());
            responseBuilder.append(" from '");
            responseBuilder.append(value.getOriginalLevel());
            responseBuilder.append("' to '");
            responseBuilder.append(value.getCurrentLevel());
            responseBuilder.append("' until ");
            responseBuilder.append(Instant.ofEpochMilli(value.getExecutionTime()));
            responseBuilder.append(" (in ");
            responseBuilder.append(value.getDuration());
            responseBuilder.append("); \n\tModification-Key: ");
            responseBuilder.append(value.getModificationKey());
            responseBuilder.append("\n");
        });

        return responseBuilder.toString();
    }
}
