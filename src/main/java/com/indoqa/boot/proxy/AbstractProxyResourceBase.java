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
package com.indoqa.boot.proxy;

import com.indoqa.httpproxy.HttpProxy;
import com.indoqa.httpproxy.HttpProxyBuilder;

import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * Use this class as base class to delegate HTTP calls to external endpoints.
 */
public abstract class AbstractProxyResourceBase {

    private static String proxy(HttpProxy httpProxy, Request req, Response res) {
        httpProxy.proxy(req.raw(), res.raw());
        return "";
    }

    protected void proxy(String proxyMountPath, String targetUrl) {
        HttpProxy httpProxy = new HttpProxyBuilder(proxyMountPath, targetUrl).build();

        String sparkPath = proxyMountPath + "/*";

        Spark.delete(sparkPath, (req, res) -> proxy(httpProxy, req, res));
        Spark.get(sparkPath, (req, res) -> proxy(httpProxy, req, res));
        Spark.head(sparkPath, (req, res) -> proxy(httpProxy, req, res));
        Spark.options(sparkPath, (req, res) -> proxy(httpProxy, req, res));
        Spark.put(sparkPath, (req, res) -> proxy(httpProxy, req, res));
        Spark.post(sparkPath, (req, res) -> proxy(httpProxy, req, res));
    }
}
