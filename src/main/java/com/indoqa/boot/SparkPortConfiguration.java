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

import static com.indoqa.boot.AbstractIndoqaBootApplication.*;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import spark.Spark;

@Named
public class SparkPortConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkPortConfiguration.class);
    private static final int SHUTDOWN_REQUEST_TIMEOUT = 250;
    private static final int SHUTDOWN_CHECK_RETRY_INTERVALL = 50;
    private static final int SHUTDOWN_EXECUTION_TIMEOUT = 500;

    @Inject
    private Environment environment;

    private static HttpURLConnection connect(URL shutdownUrl) throws IOException, ProtocolException {
        HttpURLConnection httpConnection = (HttpURLConnection) shutdownUrl.openConnection();
        HttpURLConnection.setFollowRedirects(false);
        httpConnection.setConnectTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setReadTimeout(SHUTDOWN_REQUEST_TIMEOUT);
        httpConnection.setRequestMethod("POST");
        return httpConnection;
    }

    private static boolean isPortAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);

            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);

            return true;
        } catch (IOException ioe) {
            return false;
        } finally {
            closeQuietly(ss);
            closeQuietly(ds);
        }
    }

    private static int parsePortProperty(String port) {
        try {
            return parseInt(port);
        } catch (NumberFormatException e) {
            throw new ApplicationInitializationException("Error while parsing the port property.", e);
        }
    }

    private static boolean shutdownRunningApplication(int port) {
        try {
            HttpURLConnection httpConnection = connect(new URL("http://localhost:" + port + "/shutdown"));
            httpConnection.connect();
            IOUtils.toString(httpConnection.getInputStream(), Charset.forName("utf-8"));
            httpConnection.disconnect();

            return true;
        } catch (IOException e) {
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
        this.claimPortOrShutdown(port);
        Spark.port(port);
    }

    private void claimPortOrShutdown(int port) {
        if (isPortAvailable(port)) {
            return;
        }

        // in production mode check if the port is available, otherwise stop the initialization process here
        if (!ArrayUtils.contains(this.environment.getActiveProfiles(), "dev")) {
            String msg = "The port " + port + " is in use. The initialization process stops here and the JVM is shut down.";
            LOGGER.error(msg);
            getInitializationLogger().error(msg);

            System.exit(1);
        }

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
                System.exit(1);
            }
        }
    }

    private int getPort() {
        String portProperty = this.environment.getProperty("port", DEFAULT_SPARK_PORT);
        return parsePortProperty(portProperty);
    }
}
