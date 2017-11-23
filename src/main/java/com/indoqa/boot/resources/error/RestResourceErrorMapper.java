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

import static java.util.Collections.reverseOrder;
import static java.util.Locale.US;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.resources.exception.AbstractRestResourceException;

import spark.Request;
import spark.Response;
import spark.Spark;

public final class RestResourceErrorMapper {

    private static final int RANDOM_CHARS_COUNT = 6;
    private static final BinaryOperator<Function<Exception, RestResourceErrorInfo>> MERGE_FUNCTION = (x, y) -> {
        throw new AssertionError();
    };

    private final JsonTransformer transformer;
    private Map<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>> errorProviders = new LinkedHashMap<>();

    RestResourceErrorMapper(JsonTransformer transformer) {
        this.transformer = transformer;
        registerDefaultExceptionMapping();
    }

    /**
     * Map from an exception type to a {@link RestResourceErrorInfo}. If an exception is caught, the list of mappings is processed in
     * reverse order. The first exception mapping that is assignable from the caught exception, will be applied.
     *
     * @param exception     The exception to be checked.
     * @param errorProvider The function that should be applied with an exception.
     */
    public void registerException(Class<? extends Exception> exception, Function<Exception, RestResourceErrorInfo> errorProvider) {
        this.errorProviders.put(exception, errorProvider);
    }

    void initialize() {
        sortErrorProviders();
        Spark.exception(Exception.class, (e, req, res) -> this.mapException(req, res, e));
    }

    private void registerDefaultExceptionMapping() {
        this.registerException(AbstractRestResourceException.class, (exception) -> {
            AbstractRestResourceException abstractRestResourceException = (AbstractRestResourceException) exception;
            return new RestResourceErrorInfo(abstractRestResourceException.getStatusCode(),
                abstractRestResourceException.getErrorData()
            );
        });
    }

    private void sortErrorProviders() {
        this.errorProviders = this.errorProviders
            .entrySet()
            .stream()
            .sorted(reverseOrder())
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, MERGE_FUNCTION, LinkedHashMap::new));
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
