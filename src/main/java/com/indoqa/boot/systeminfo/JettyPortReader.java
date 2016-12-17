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

import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;

import spark.Service;
import spark.Spark;
import spark.embeddedserver.jetty.EmbeddedJettyServer;

/*default*/ class JettyPortReader {

    private static final Logger LOGGER = getLogger(JettyPortReader.class);

    private JettyPortReader() {
        // hide utility class constructor
    }

    public static int getPort() {
        int sparkPort = Spark.port();

        // if no random port is set, Spark.port() returns the correct port
        if (sparkPort != 0) {
            return sparkPort;
        }

        // otherwise only Jetty knows ... (use reflection and private accessors to find the required information)
        try {
            Service sparkService = getSparkService();
            EmbeddedJettyServer embeddedJettyServer = getEmbeddedJettyServer(sparkService);
            AbstractNetworkConnector connector = getConnector(embeddedJettyServer);
            return connector.getPort();
        } catch (Exception e) {
            LOGGER.error("Error while reading Jetty server port.", e);
        }
        return 0;
    }

    private static AbstractNetworkConnector getConnector(EmbeddedJettyServer embeddedJettyServer)
            throws NoSuchFieldException, IllegalAccessException {
        Field serverField = embeddedJettyServer.getClass().getDeclaredField("server");
        serverField.setAccessible(true);
        Server server = (Server) serverField.get(embeddedJettyServer);
        return (AbstractNetworkConnector) server.getConnectors()[0];
    }

    private static EmbeddedJettyServer getEmbeddedJettyServer(Service sparkService)
            throws NoSuchFieldException, IllegalAccessException {
        Field embeddedJettyServerField = sparkService.getClass().getDeclaredField("server");
        embeddedJettyServerField.setAccessible(true);
        return (EmbeddedJettyServer) embeddedJettyServerField.get(sparkService);
    }

    private static Service getSparkService() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method getInstanceMethod = Spark.class.getDeclaredMethod("getInstance");
        getInstanceMethod.setAccessible(true);
        return (Service) getInstanceMethod.invoke(null);
    }
}
