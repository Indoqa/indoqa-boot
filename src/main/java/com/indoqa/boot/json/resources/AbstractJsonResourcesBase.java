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
package com.indoqa.boot.json.resources;

import javax.inject.Inject;

import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.resources.AbstractResourcesBase;

import spark.ResponseTransformer;
import spark.Route;
import spark.Spark;

public abstract class AbstractJsonResourcesBase extends AbstractResourcesBase {

    protected static final String CONTENT_TYPE_JSON = "application/json";

    @Inject
    private JsonTransformer transformer;

    public void delete(String path, Route route) {
        this.delete(path, route, this.transformer);
    }

    public void delete(String path, Route route, ResponseTransformer responseTransformer) {
        Spark.delete(this.resolvePath(path), CONTENT_TYPE_JSON, route, responseTransformer);
    }

    public void get(String path, Route route) {
        this.get(path, route, this.transformer);
    }

    public void get(String path, Route route, ResponseTransformer responseTransformer) {
        Spark.get(this.resolvePath(path), CONTENT_TYPE_JSON, route, responseTransformer);
    }

    public void post(String path, Route route) {
        this.post(path, route, this.transformer);
    }

    public void post(String path, Route route, ResponseTransformer responseTransformer) {
        Spark.post(this.resolvePath(path), CONTENT_TYPE_JSON, route, responseTransformer);
    }

    public void put(String path, Route route) {
        this.put(path, route, this.transformer);
    }

    public void put(String path, Route route, ResponseTransformer responseTransformer) {
        Spark.put(this.resolvePath(path), CONTENT_TYPE_JSON, route, responseTransformer);
    }

    protected JsonTransformer getTransformer() {
        return this.transformer;
    }
}
