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

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.AbstractJsonResourcesBase;
import com.indoqa.boot.systeminfo.SystemInfo;

import spark.Response;

public class SystemInfoResource extends AbstractJsonResourcesBase {

    @Inject
    private SystemInfo systemInfo;

    @PostConstruct
    public void mount() {
        this.get("/system-info", (request, response) -> this.sendSystemInfo(response));
    }

    private SystemInfo sendSystemInfo(Response response) {
        if (this.systemInfo.isInitialized()) {
            return this.systemInfo;
        }
        response.status(HTTP_NOT_FOUND);
        return null;
    }
}
