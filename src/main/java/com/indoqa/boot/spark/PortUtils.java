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
package com.indoqa.boot.spark;

import static com.indoqa.boot.AbstractIndoqaBootApplication.getInitializationLogger;
import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.profile.ProfileDetector;

public final class PortUtils {

    private PortUtils() {
        // hide utility class constructor
    }

    public static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            return true;
        } catch (IOException ioe) { // NOSONAR
            return false;
        } finally {
            closeQuietly(ss);
        }
    }

    protected static void claimPortOrShutdown(Environment environment, int port, String propertyName, Logger logger) {
        if (ProfileDetector.isProd(environment)) {
            String msg = "The " + propertyName + " " + port
                + " is in use. The initialization process stops here and the JVM is shut down.";
            logger.error(msg);
            getInitializationLogger().error(msg);
            terminate();
        }
    }

    protected static int parseIntegerProperty(String value, String name) {
        try {
            return parseInt(value);
        } catch (NumberFormatException e) {
            throw new ApplicationInitializationException("Error while parsing the property '" + name + "'. value=" + value, e);
        }
    }

    protected static void terminate() {
        System.exit(1);
    }
}
