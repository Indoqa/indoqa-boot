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

import static com.indoqa.boot.logging.InitializationLogger.getInitializationLogger;
import static com.indoqa.boot.spark.PortUtils.*;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import com.indoqa.boot.profile.ProfileDetector;

public abstract class AbstractSparkService {

    private static final Logger LOGGER = getLogger(SparkAdminService.class);

    protected static final String PROPERTY_PORT = "port";
    protected static final String PROPERTY_ADMIN_PORT = "admin.port";

    private static final String DEFAULT_SPARK_PORT = "4567";
    private static final String DEFAULT_ADMIN_PORT = "34567";

    private static final int SHUTDOWN_REQUEST_TIMEOUT = 250;
    private static final int SHUTDOWN_CHECK_RETRY_INTERVALL = 50;
    private static final int SHUTDOWN_EXECUTION_TIMEOUT = 500;

    @Inject
    protected Environment environment;

    private static HttpURLConnection connect(URL shutdownUrl) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) shutdownUrl.openConnection();
        HttpURLConnection.setFollowRedirects(false);
        httpConnection.setConnectTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setReadTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setRequestMethod("POST");
        return httpConnection;
    }

    private static void shutdownRunningApplication(int port) {
        URL shutdownUrl = null;
        try {
            shutdownUrl = new URL("http://localhost:" + port + "/shutdown");

            HttpURLConnection httpConnection = connect(shutdownUrl);
            httpConnection.connect();
            IOUtils.toString(httpConnection.getInputStream(), UTF_8);
            httpConnection.disconnect();

            LOGGER.info("A shutdown request was sent successfully to {}", shutdownUrl);
        } catch (IOException e) {
            LOGGER.info("A shutdown request to {} failed.", shutdownUrl);
        }
    }

    private static void sleep(int sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    protected void claimPortOrShutdown(int checkPort, int shutdownPort) {
        if (isPortAvailable(checkPort)) {
            return;
        }

        // in production mode check if the port is available, otherwise stop the initialization process of THIS application here
        if (ProfileDetector.isProd(this.environment)) {
            String msg = "The port " + checkPort + " is in use. The initialization process stops here and the JVM is shut down.";
            LOGGER.error(msg);
            getInitializationLogger().error(msg);
            System.exit(1);
        }

        // REST request to shut down the application that uses the port
        // This will only work for an Indoqa-Boot application.
        shutdownRunningApplication(shutdownPort);

        // check if the other application was shut down, otherwise stop the initialization process here
        long runUntil = currentTimeMillis() + SHUTDOWN_EXECUTION_TIMEOUT;
        while (true) {
            if (isPortAvailable(checkPort)) {
                break;
            }

            sleep(SHUTDOWN_CHECK_RETRY_INTERVALL);

            if (runUntil < currentTimeMillis()) {
                LOGGER.error(
                    "The port " + checkPort + " is still in use. The initialization process stops here and the JVM is shut down.");
                System.exit(1);
            }
        }
    }

    protected int getAdminPort() {
        String portProperty = this.environment.getProperty(PROPERTY_ADMIN_PORT, DEFAULT_ADMIN_PORT);
        return parseIntegerProperty(portProperty, PROPERTY_ADMIN_PORT);
    }

    protected int getPort() {
        String portProperty = this.environment.getProperty(PROPERTY_PORT, DEFAULT_SPARK_PORT);
        return parseIntegerProperty(portProperty, PROPERTY_PORT);
    }
}
