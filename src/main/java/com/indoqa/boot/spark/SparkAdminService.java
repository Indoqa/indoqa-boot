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

import static org.slf4j.LoggerFactory.getLogger;
import static spark.globalstate.ServletFlag.isRunningFromServlet;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;

import spark.Service;

/**
 * An additional Spark service for admin resources that should be kept separately from the application HTTP resources.
 */
public final class SparkAdminService extends AbstractSparkService {

    private static final Logger LOGGER = getLogger(SparkAdminService.class);

    private Service service;

    @PostConstruct
    public void initialize() {
        if (isRunningFromServlet()) {
            return;
        }

        int adminPort = this.getAdminPort();
        if (adminPort == -1) {
            LOGGER.info("The admin service is deactivated because the property admin.port is set to -1.");
            return;
        }

        this.claimPortOrShutdown(adminPort, adminPort);

        this.service = Service.ignite();
        this.service.port(adminPort);
    }

    public Service instance() {
        return this.service;
    }

    public boolean isAvailable() {
        return this.service != null;
    }
}
