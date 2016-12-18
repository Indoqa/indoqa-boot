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
import static java.lang.Boolean.*;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import spark.Service;

public final class SparkAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkAdminService.class);

    private static final String PROPERTY_ADMIN_PORT = "admin-port";
    private static final String PROPERTY_SEPARATE_ADMIN_SERVICE = "admin-service.separate";
    private static final String DEFAULT_ADMIN_PORT = "34567";

    private Service service;

    @Inject
    private Environment environment;

    @PostConstruct
    public void initialize() {
        if (isRunningFromServlet()) {
            return;
        }

        if (!this.separateAdminService()) {
            LOGGER.info(
                "There is no separate admin service because the property '{}' is set to false.", PROPERTY_SEPARATE_ADMIN_SERVICE);
            return;
        }

        int adminPort = this.getAdminPort();
        claimPortOrShutdown(this.environment, adminPort, PROPERTY_ADMIN_PORT, LOGGER);

        this.service = Service.ignite();
        this.service.port(adminPort);
    }

    public Service instance() {
        return this.service;
    }

    public boolean isAvailable() {
        return this.service != null;
    }

    private int getAdminPort() {
        String portProperty = this.environment.getProperty(PROPERTY_ADMIN_PORT, DEFAULT_ADMIN_PORT);
        return parseIntegerProperty(portProperty, PROPERTY_ADMIN_PORT);
    }

    private boolean separateAdminService() {
        return parseBoolean(this.environment.getProperty(PROPERTY_SEPARATE_ADMIN_SERVICE, TRUE.toString()));
    }
}
