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

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.spark.SparkAdminService;

import spark.Route;
import spark.Service;

public abstract class AbstractActuatorResources extends AbstractJsonResourcesBase {

    @Inject
    private SparkAdminService sparkAdminService;

    protected void getActuator(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().get(path, route, this.getTransformer());
        } else {
            this.get(path, route);
        }
    }

    protected Service getSparkAdminService() {
        return this.sparkAdminService.instance();
    }

    protected boolean isAdminServiceAvailable() {
        return this.sparkAdminService.isAvailable();
    }

    protected void postActuator(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().post(path, route, this.getTransformer());
        } else {
            this.post(path, route);
        }
    }
}
