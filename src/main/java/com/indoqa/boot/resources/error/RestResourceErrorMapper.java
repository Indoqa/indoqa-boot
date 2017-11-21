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
package com.indoqa.boot.resources.error;

import static java.util.Locale.US;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;

import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.resources.exception.AbstractRestResourceException;

import spark.Request;
import spark.Response;
import spark.Spark;

public class RestResourceErrorMapper {

    private static final int RANDOM_CHARS_COUNT = 6;

    private final JsonTransformer transformer;
    private final LinkedHashMap<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>> errorProviders = new LinkedHashMap<>();

    public RestResourceErrorMapper(JsonTransformer transformer) {
        this.transformer = transformer;
        this.registerException(AbstractRestResourceException.class, (exception) -> {
            AbstractRestResourceException abstractRestResourceException = (AbstractRestResourceException) exception;
            return new RestResourceErrorInfo(abstractRestResourceException.getStatusCode(),
                abstractRestResourceException.getErrorData()
            );
        });
    }

    public void registerException(Class<? extends Exception> exception, Function<Exception, RestResourceErrorInfo> errorProvider) {
        this.errorProviders.put(exception, errorProvider);
    }

    @PostConstruct
    public void initialize() {
        Spark.exception(Exception.class, (e, req, res) -> this.mapException(req, res, e));
    }

    private RestResourceError buildError(Exception exception) {
        final String id = randomAlphanumeric(RANDOM_CHARS_COUNT).toUpperCase(US);
        final Instant now = Instant.now();
        final String message = exception.getMessage();
        final RestResourceErrorInfo errorInfo = this.errorProviders
            .entrySet()
            .stream()
            .filter(providerEntry -> providerEntry.getKey().isAssignableFrom(exception.getClass()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(e -> new RestResourceErrorInfo(null, null))
            .apply(exception);

        return RestResourceError.build(id, now, message, errorInfo);
    }

    private void mapException(Request req, Response res, Exception e) {
        RestResourceError error = buildError(e);

        RestResourceErrorLogger.logException(req, error, e);

        res.status(error.getStatus());
        res.body(this.transformer.render(error));
    }
}
