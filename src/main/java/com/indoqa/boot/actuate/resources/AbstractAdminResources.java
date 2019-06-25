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

import static java.lang.Boolean.FALSE;

import javax.inject.Inject;

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.spark.SparkAdminService;
import org.springframework.core.env.Environment;

import spark.Route;
import spark.Service;
import spark.Spark;

public abstract class AbstractAdminResources extends AbstractJsonResourcesBase {

    protected static final String CONTENT_TYPE_HTML = "text/html; charset=utf-8";
    private static final String PROPERTY_ADMIN_ENABLED_VIA_DEFAULT_SERVICE = "admin.enabled-via-default-service";
    private static final String ADMIN_BASE_PATH = "/admin";

    @Inject
    private SparkAdminService sparkAdminService;

    @Inject
    private Environment environment;

    protected static String resolveAdminPath(String path) {
        return new StringBuilder(ADMIN_BASE_PATH).append(path).toString();
    }

    protected void getActuator(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().get(path, CONTENT_TYPE_JSON, route, this.getTransformer());
        }

        else if (this.isEnabledViaDefaultService()) {
            this.get(resolveAdminPath(path), route);
        }
    }

    protected void headActuator(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().head(path, route);
        }

        else if (this.isEnabledViaDefaultService()) {
            this.head(resolveAdminPath(path), route);
        }
    }

    protected void putActuator(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().put(path, CONTENT_TYPE_JSON, route, this.getTransformer());
        }

        else if (this.isEnabledViaDefaultService()) {
            this.put(resolveAdminPath(path), route);
        }
    }

    protected void getActuatorHtml(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.getSparkAdminService().get(path, CONTENT_TYPE_HTML, route);
        }

        else if (this.isEnabledViaDefaultService()) {
            Spark.get(resolveAdminPath(path), CONTENT_TYPE_HTML, route);
        }
    }

    protected void putActuatorHtml(String path, Route route) {
        if (this.isAdminServiceAvailable()) {
            this.sparkAdminService.instance().put(path, CONTENT_TYPE_HTML, route, this.getTransformer());
        }

        else if (this.isEnabledViaDefaultService()) {
            Spark.put(resolveAdminPath(path), CONTENT_TYPE_HTML, route);
        }
    }

    protected Service getSparkAdminService() {
        return this.sparkAdminService.instance();
    }

    protected boolean isAdminServiceAvailable() {
        return this.sparkAdminService.isAvailable();
    }

    protected Boolean isEnabledViaDefaultService() {
        return this.environment.getProperty(PROPERTY_ADMIN_ENABLED_VIA_DEFAULT_SERVICE, Boolean.class, FALSE);
    }
}
