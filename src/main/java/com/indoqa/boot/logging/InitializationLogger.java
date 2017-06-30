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
package com.indoqa.boot.logging;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * Get access to a special logger for initialization information. The idea is that the log4j configuration should treat those logging
 * messages differently, e.g. by printing the plain log messages without timestamps, threads, etc.
 */
public final class InitializationLogger {

    private static final Logger INIT_LOGGER = getLogger(InitializationLogger.class.getName());

    private InitializationLogger() {
        // hide utility class constructor
    }

    public static Logger getInitializationLogger() {
        return INIT_LOGGER;
    }
}
