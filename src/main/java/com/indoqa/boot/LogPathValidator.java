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
package com.indoqa.boot;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.lang3.StringUtils;

/*default*/final class LogPathValidator {

    private static final PrintStream ERR = System.err;
    private static final String LOG_PATH_PROPERTY = "log-path";

    private LogPathValidator() {
        // hide utility class constructor
    }

    static void checkLogDir() {
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
            ERR.println("Application initilization error: The log-path '" + logPathFile.getCanonicalPath() + "' does not exist.");
        } catch (IOException e) { // NOSONAR
            ERR.println("Application initilization error: " + e.getMessage());
        }
        exit();
    }

    private static void checkLogpathIsDirectory(File logPathFile) {
        if (logPathFile.isDirectory()) {
            return;
        }

        try {
            ERR.println("Application initilization error: The log-path '" + logPathFile.getCanonicalPath() + "' is not a directory.");
        } catch (IOException e) { // NOSONAR
            ERR.println("Application initilization error: " + e.getMessage());
        }
        exit();
    }

    private static void checkLogPathPropertyIsAvailable(String logPath) {
        if (!StringUtils.isBlank(logPath)) {
            return;
        }

        ERR.println("Application  initilization error: The system property '" + LOG_PATH_PROPERTY + "' is not set.");
        exit();
    }

    private static void exit() {
        System.exit(1);
    }

}
