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
package com.indoqa.boot.application;

import static com.indoqa.boot.logging.InitializationLogger.getInitializationLogger;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/*default*/final class LogPathValidator {

    private static final Logger INIT_LOGGER = getInitializationLogger();
    private static final String LOG_PATH_PROPERTY = "log-path";

    private LogPathValidator() {
        // hide utility class constructor
    }

    public static void checkLogDir() {
        String logPath = System.getProperty(LOG_PATH_PROPERTY);
        checkLogPathPropertyIsAvailable(logPath);

        File logPathFile = new File(logPath);
        checkLogPathExists(logPathFile);
        checkLogpathIsDirectory(logPathFile);
    }

    private static void checkLogPathExists(File logPathFile) {
        if (logPathFile.exists()) {
            return;
        }

        try {
            getInitializationLogger()
                .error("Application initilization error: The log-path '" + logPathFile.getCanonicalPath() + "' does not exist.");
        } catch (IOException e) { // NOSONAR (the stacktrace is of no value here)
            getInitializationLogger().error("Application initilization error: " + e.getMessage());
        }
        terminate();
    }

    private static void checkLogpathIsDirectory(File logPathFile) {
        if (logPathFile.isDirectory()) {
            return;
        }

        try {
            INIT_LOGGER
                .error("Application initilization error: The log-path '" + logPathFile.getCanonicalPath() + "' is not a directory.");
        } catch (IOException e) { // NOSONAR (the stacktrace is of no value here)
            INIT_LOGGER.error("Application initilization error: " + e.getMessage());
        }
        terminate();
    }

    private static void checkLogPathPropertyIsAvailable(String logPath) {
        if (!StringUtils.isBlank(logPath)) {
            return;
        }

        INIT_LOGGER.error("Application initilization error: Make sure that the system property '" + LOG_PATH_PROPERTY + "' is set.");
        terminate();
    }

    private static void terminate() {
        System.exit(1);
    }
}
