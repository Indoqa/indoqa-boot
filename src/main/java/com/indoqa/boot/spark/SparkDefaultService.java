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

import static com.indoqa.boot.spark.PortUtils.*;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import spark.Spark;

public class SparkDefaultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkDefaultService.class);
    private static final String DEFAULT_SPARK_PORT = "4567";
    private static final int SHUTDOWN_REQUEST_TIMEOUT = 250;
    private static final int SHUTDOWN_CHECK_RETRY_INTERVALL = 50;
    private static final int SHUTDOWN_EXECUTION_TIMEOUT = 500;
    private static final String PROPERTY_PORT = "port";

    @Inject
    private Environment environment;

    private static HttpURLConnection connect(URL shutdownUrl) throws IOException {
        HttpURLConnection httpConnection = (HttpURLConnection) shutdownUrl.openConnection();
        HttpURLConnection.setFollowRedirects(false);
        httpConnection.setConnectTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setReadTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setRequestMethod("POST");
        return httpConnection;
    }

    private static boolean shutdownRunningApplication(int port) {
        try {
            HttpURLConnection httpConnection = connect(new URL("http://localhost:" + port + "/shutdown"));
            httpConnection.connect();
            IOUtils.toString(httpConnection.getInputStream(), UTF_8);
            httpConnection.disconnect();

            return true;
        } catch (IOException e) { // NOSONAR
            return false;
        }
    }

    private static void sleep(int sleep) {
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @PostConstruct
    public void initialize() {
        // stop if the application runs within a servlet container
        if (isRunningFromServlet()) {
            return;
        }

        int port = this.getPort();
        this.claimPortOrShutdownOtherApplication(port);
        Spark.port(port);
    }

    private void claimPortOrShutdownOtherApplication(int port) {
        if (isPortAvailable(port)) {
            return;
        }

        // in production mode check if the port is available, otherwise stop the initialization process here
        claimPortOrShutdown(this.environment, port, PROPERTY_PORT, LOGGER);

        // REST request to shut down the application that uses the port
        // This will only work for an Indoqa Boot application that runs with the 'dev' profile.
        boolean successfullShutdownRequest = shutdownRunningApplication(port);
        if (successfullShutdownRequest) {
            LOGGER.info("A shutdown request was sent successfully to the application running at port " + port + ".");
        }

        // check if the other application was shut down, otherwise stop the initialization process here
        long runUntil = currentTimeMillis() + SHUTDOWN_EXECUTION_TIMEOUT;
        while (true) {
            if (isPortAvailable(port)) {
                break;
            }

            sleep(SHUTDOWN_CHECK_RETRY_INTERVALL);

            if (runUntil < currentTimeMillis()) {
                LOGGER.error("The port " + port + " is still in use. The initialization process stops here and the JVM is shut down.");
                terminate();
            }
        }
    }

    private int getPort() {
        String portProperty = this.environment.getProperty(PROPERTY_PORT, DEFAULT_SPARK_PORT);
        return parseIntegerProperty(portProperty, PROPERTY_PORT);
    }
}
