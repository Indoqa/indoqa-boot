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

import javax.inject.Inject;

import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.spark.SparkAdminService;

import spark.Route;

public abstract class AbstractActuatorResources {

    @Inject
    private SparkAdminService sparkAdminService;

    @Inject
    private JsonTransformer jsonTransformer;

    protected void get(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().get(path, route, this.jsonTransformer);
        } else {
            this.get(path, route);
        }
    }

    protected SparkAdminService getSparkAdminService() {
        return this.sparkAdminService;
    }

    protected boolean isAdminServiceAvailable() {
        return this.sparkAdminService.isAvailable();
    }
}
