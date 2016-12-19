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
package com.indoqa.boot.resource;

import static java.net.HttpURLConnection.*;
import static org.slf4j.LoggerFactory.getLogger;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;

import com.indoqa.boot.AbstractJsonResourcesBase;
import com.indoqa.boot.json.JsonTransformer;
import com.indoqa.boot.spark.SparkAdminService;

import spark.Response;

public class ShutdownResource extends AbstractJsonResourcesBase {

    private static final Logger LOGGER = getLogger(ShutdownResource.class);
    private static final int SHUTDOWN_DELAY = 50;

    @Inject
    private SparkAdminService sparkAdminService;

    @Inject
    private JsonTransformer jsonTransformer;

    private static String shutdown(Response response) {
        if (isRunningFromServlet()) {
            LOGGER.warn(
                "The shutdown resource received a request. Since the application runs within a servlet "
                    + "container, the application WILL NOT shut down.");
            response.status(HTTP_FORBIDDEN);
            return null;
        }

        LOGGER.warn("Triggered by a REST request, the application is going to shut down.");

        // delay the shutdown so that the REST response can be sent
        new java.util.Timer().schedule(new ShutdownTask(), SHUTDOWN_DELAY);

        response.status(HTTP_ACCEPTED);
        return null;
    }

    @PostConstruct
    public void mount() {
        if (this.sparkAdminService.isAvailable()) {
            this.sparkAdminService.instance().post("/shutdown", (req, res) -> shutdown(res), this.jsonTransformer);
        } else {
            this.post("/shutdown", (req, res) -> shutdown(res));
        }
    }

    private static class ShutdownTask extends TimerTask {

        @Override
        public void run() {
            LOGGER.warn("The JVM is shut down.");
            System.exit(0);
        }
    }
}
