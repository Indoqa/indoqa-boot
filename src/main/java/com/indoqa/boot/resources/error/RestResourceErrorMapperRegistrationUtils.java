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

import java.util.function.Consumer;

import com.indoqa.boot.json.transformer.JsonTransformer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class RestResourceErrorMapperRegistrationUtils {

    private RestResourceErrorMapperRegistrationUtils() {
        // hide utility class constructor
    }

    /**
     * Register a {@link RestResourceErrorMapper} with additional exception mappings at Spring and Spark. Use this method in the
     * {@link com.indoqa.boot.application.StartupLifecycle#didInitializeSpring(AnnotationConfigApplicationContext)}
     * phase of the application's startup lifecycle.
     *
     * @param context             The Spring application context.
     * @param errorMapperConsumer Provide additional error information for custom application exceptions.
     */
    public static void registerRestResourceErrorMapper(AnnotationConfigApplicationContext context,
        Consumer<RestResourceErrorMapper> errorMapperConsumer) {
        JsonTransformer jsonTransformer = context.getBean(JsonTransformer.class);

        RestResourceErrorMapper errorMapper = new RestResourceErrorMapper(jsonTransformer);
        if (errorMapperConsumer != null) {
            errorMapperConsumer.accept(errorMapper);
        }
        errorMapper.initialize();

        context.getBeanFactory().registerSingleton(RestResourceErrorMapper.class.getName(), errorMapper);
    }

    /**
     * Register a {@link RestResourceErrorMapper} at Spring and Spark. Use this method in the
     * {@link com.indoqa.boot.application.StartupLifecycle#didInitializeSpring(AnnotationConfigApplicationContext)}
     * phase of the application's startup lifecycle.
     *
     * @param context The Spring application context.
     */
    public static void registerRestResourceErrorMapper(AnnotationConfigApplicationContext context) {
        registerRestResourceErrorMapper(context, null);
    }
}
