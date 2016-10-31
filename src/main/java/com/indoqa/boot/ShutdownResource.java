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

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import spark.Response;

@Named
@Profile("dev")
public class ShutdownResource extends AbstractJsonResourcesBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownResource.class);
    private static final int SHUTDOWN_DELAY = 50;

    private static String shutdown(Response response) {
        if (isRunningFromServlet()) {
            LOGGER.warn("The shutdown resource received a request. Since the application runs within a servlet "
                + "container, the application WILL NOT shut down.");
        }

        LOGGER.warn("Triggered by a REST request, the application is going to shut down in " + SHUTDOWN_DELAY + " ms.");

        // delay the shutdown so that the REST response can be sent
        new java.util.Timer().schedule(new ShutdownTask(), SHUTDOWN_DELAY);

        response.status(HTTP_ACCEPTED);
        return "";
    }

    @PostConstruct
    public void mount() {
        this.post("/shutdown", (req, res) -> shutdown(res));
    }

    private static class ShutdownTask extends TimerTask {

        @Override
        public void run() {
            LOGGER.warn("The JVM is shut down.");
            System.exit(0);
        }
    }
}
