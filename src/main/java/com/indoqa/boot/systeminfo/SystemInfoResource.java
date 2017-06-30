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
package com.indoqa.boot.systeminfo;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.spark.SparkAdminService;

import spark.Response;

/**
 * An admin resource that provides information about the application state and environment (e.g. used Spring properties, etc.).
 */
public class SystemInfoResource extends AbstractJsonResourcesBase {

    @Inject
    private SystemInfo systemInfo;

    @Inject
    private BasicSystemInfo reducedSystemInfo;

    @Inject
    private JsonTransformer jsonTransformer;

    @Inject
    private SparkAdminService sparkAdminService;

    @PostConstruct
    public void mount() {
        if (this.sparkAdminService.isAvailable()) {
            this.get("/system-info", (request, response) -> this.sendReducedSystemInfo(response));
            this.sparkAdminService
                .instance()
                .get("/system-info", (request, response) -> this.sendSystemInfo(response), this.jsonTransformer);
        }

        else {
            this.get("/system-info", (request, response) -> this.sendSystemInfo(response));
        }
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
