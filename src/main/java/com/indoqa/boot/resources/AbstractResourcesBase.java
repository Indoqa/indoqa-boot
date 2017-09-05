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
package com.indoqa.boot.resources;

import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.time.Instant;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.ApplicationInitializationException;
import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.resources.error.RestResourceError;
import com.indoqa.boot.resources.error.RestResoureErrorLogger;
import com.indoqa.boot.resources.exception.AbstractRestResourceException;
import com.indoqa.boot.resources.exception.HttpStatusCode;

import spark.Request;
import spark.Response;
import spark.Spark;

/**
 * A base implementation that exposes the method {@link #getResourceBase()} to conveniently mount resources to a particular base path.
 */
public abstract class AbstractResourcesBase {

    private static final String DEFAULT_BASE_PATH = "";

    @Inject
    private JsonTransformer transformer;

    public AbstractResourcesBase() {
        super();
    }

    @PostConstruct
    public void initialize() {
        this.checkResourceBase();
        this.mapException();
    }

    protected CharSequence getResourceBase() {
        return DEFAULT_BASE_PATH;
    }

    protected String resolvePath(CharSequence path) {
        if (!startsWith(path, "/")) {
            throw new ApplicationInitializationException(
                "A Spark resource cannot be mounted to '" + path + "'. The path has to start with a '/'.");
        }

        return new StringBuilder(this.getResourceBase()).append(path).toString();
    }

    private RestResourceError buildError(Exception exception) {
        final String uuid = UUID.randomUUID().toString();
        final Instant now = Instant.now();

        HttpStatusCode status = HttpStatusCode.INTERNAL_SERVER_ERROR;

        String error = exception.getMessage();
        Object payload = null;

        if (exception instanceof AbstractRestResourceException) {
            AbstractRestResourceException restResourceException = (AbstractRestResourceException) exception;

            status = restResourceException.getStatusCode();
            payload = restResourceException.getErrorData();
        }

        return RestResourceError.build(status.getCode(), now, uuid, error, payload);
    }

    private void checkResourceBase() {
        CharSequence resourceBase = this.getResourceBase();
        if (isBlank(resourceBase)) {
            return;
        }

        if (!startsWith(resourceBase, "/")) {
            throw new ApplicationInitializationException(
                "The Spark resource base path '" + resourceBase + "' is invalid. It must start with a '/'.");
        }

        if (endsWith(resourceBase, "/")) {
            throw new ApplicationInitializationException(
                "The Spark resource base path '" + resourceBase + "' is invalid. It must not end with a '/'.");
        }
    }

    private void mapException() {
        Spark.exception(Exception.class, (e, req, res) -> this.mapException(req, res, e));
    }

    private void mapException(Request req, Response res, Exception e) {
        RestResourceError error = this.buildError(e);

        RestResoureErrorLogger.logException(req, error, e);

        res.status(error.getStatus());
        res.body(this.transformer.render(error));
    }
}
