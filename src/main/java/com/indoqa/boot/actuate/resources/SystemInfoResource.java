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
package com.indoqa.boot.actuate.resources;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import com.indoqa.boot.actuate.systeminfo.BasicSystemInfo;
import com.indoqa.boot.actuate.systeminfo.SystemInfo;
import com.indoqa.boot.json.transformer.JsonTransformer;

import spark.Response;
import spark.Spark;

/**
 * An admin resource that provides information about the application state and environment (e.g. used Spring properties, etc.).
 */
public class SystemInfoResource extends AbstractAdminResources {

    @Inject
    private SystemInfo systemInfo;

    @Inject
    private BasicSystemInfo reducedSystemInfo;

    @Inject
    private JsonTransformer jsonTransformer;

    @PostConstruct
    public void mount() {
        this.getActuator("/system-info", (request, response) -> this.sendSystemInfo(response));

        // minimal systemInfo exposed via the business application REST service
        Spark.get("/system-info", (request, response) -> this.sendReducedSystemInfo(response), this.jsonTransformer);
    }

    private BasicSystemInfo sendReducedSystemInfo(Response response) {
        if (this.reducedSystemInfo.isInitialized()) {
            return this.reducedSystemInfo;
        }

        response.status(HTTP_NOT_FOUND);
        return null;
    }

    private SystemInfo sendSystemInfo(Response response) {
        if (this.systemInfo.isInitialized()) {
            return this.systemInfo;
        }

        response.status(HTTP_NOT_FOUND);
        return null;
    }
}
