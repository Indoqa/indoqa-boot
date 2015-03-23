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
package com.indoqa.boot;

import java.io.IOException;

import javax.inject.Named;

import spark.ResponseTransformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Named
public class JsonTransformer implements ResponseTransformer {

    private ObjectMapper mapper;

    public JsonTransformer() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String render(Object model) {
        try {
            return this.mapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new JsonTransformerException("Can't marshal object as JSON string.", e);
        }
    }

    public <T> T toObject(String json, Class<T> type) {
        try {
            return this.mapper.readValue(json, type);
        } catch (IOException e) {
            throw new JsonTransformerException("Can't unmarshal object from JSON string:\n" + json, e);
        }
    }

    public static class JsonTransformerException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public JsonTransformerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
