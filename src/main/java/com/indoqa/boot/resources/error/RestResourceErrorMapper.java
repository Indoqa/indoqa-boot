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

import static java.time.Instant.now;
import static java.util.Locale.US;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.resources.exception.AbstractRestResourceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import spark.Request;
import spark.Response;
import spark.Spark;

public final class RestResourceErrorMapper {

    private static final int RANDOM_CHARS_COUNT = 6;

    private static final Comparator<Map.Entry<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>>> EXCEPTION_COMPARATOR = (e1, e2) -> {
        if (e1.getKey().isAssignableFrom(e2.getKey())) {
            return 1;
        }
        if (e2.getKey().isAssignableFrom(e1.getKey())) {
            return -1;
        }
        return StringUtils.compare(e1.getKey().getName(), e2.getKey().getName());
    };

    private final JsonTransformer transformer;
    private final List<SortedSet<Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>>>> mappings = new ArrayList<>();

    RestResourceErrorMapper(JsonTransformer transformer) {
        this.transformer = transformer;
        registerDefaultExceptionMapping();
    }

    private static boolean haveSpecificSuperExceptionType(Class<?> e1, Class<?> e2) {
        while (!e1.isAssignableFrom(e2)) {
            e1 = e1.getSuperclass();
        }
        return !e1.equals(Exception.class) && !e1.equals(RuntimeException.class);
    }

    /**
     * Map from an exception type to a {@link RestResourceErrorInfo}. The list is sorted in the way that more specific exceptions
     * will match first.
     *
     * @param exceptionType The exception to be checked.
     * @param errorProvider The function that should be applied with an exception.
     */
    public void registerException(Class<? extends Exception> exceptionType, Function<Exception, RestResourceErrorInfo> errorProvider) {
        Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>> currentMapping = Pair.of(exceptionType,
            errorProvider
        );

        Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>> matchingMapping = null;
        for (SortedSet<Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>>> eachBucket : this.mappings) {
            // find out if the currentMapping exception belongs to the bucket
            for (Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>> eachMapping : eachBucket) {
                Class<? extends Exception> mappingType = eachMapping.getKey();
                boolean haveSpecificSuperExceptionType = haveSpecificSuperExceptionType(mappingType, currentMapping.getKey());

                if (exceptionType.isAssignableFrom(mappingType) || mappingType.isAssignableFrom(exceptionType)
                    || haveSpecificSuperExceptionType) {
                    matchingMapping = currentMapping;
                    break;
                }
            }

            if (matchingMapping != null) {
                eachBucket.add(matchingMapping);
                break;
            }
        }

        // if there was no matching bucket, create a new one
        if (matchingMapping == null) {
            // use a set that is sorted by the class hierarchy (more specific exceptions come first)
            SortedSet<Pair<Class<? extends Exception>, Function<Exception, RestResourceErrorInfo>>> newBucket = new TreeSet<>(
                EXCEPTION_COMPARATOR);
            newBucket.add(currentMapping);
            this.mappings.add(newBucket);
        }
    }

    void initialize() {
        Spark.exception(Exception.class, (e, req, res) -> this.mapException(req, res, e));
    }

    RestResourceError buildError(Exception exception) {
        final String id = randomAlphanumeric(RANDOM_CHARS_COUNT).toUpperCase(US);
        final Instant now = now();
        final String message = defaultIfBlank(exception.getMessage(), exception.getClass().getSimpleName());
        final RestResourceErrorInfo errorInfo = this.mappings
            .stream()
            .flatMap(Collection::stream)
            .filter(providerEntry -> providerEntry.getKey().isAssignableFrom(exception.getClass()))
            .map(Pair::getValue)
            .findFirst()
            .orElse(e -> new RestResourceErrorInfo(null, null))
            .apply(exception);

        return RestResourceError.build(id, now, message, errorInfo);
    }

    private void registerDefaultExceptionMapping() {
        this.registerException(AbstractRestResourceException.class, (exception) -> {
            AbstractRestResourceException abstractRestResourceException = (AbstractRestResourceException) exception;
            return new RestResourceErrorInfo(abstractRestResourceException.getStatusCode(),
                abstractRestResourceException.getErrorData()
            );
        });
    }

    private void mapException(Request req, Response res, Exception e) {
        RestResourceError error = buildError(e);

        RestResourceErrorLogger.logException(req, error, e);

        res.status(error.getStatus());
        res.body(this.transformer.render(error));
    }
}
