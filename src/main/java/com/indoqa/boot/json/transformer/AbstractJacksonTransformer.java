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
package com.indoqa.boot.json.transformer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This base implementation of a {@link JsonTransformer} uses Jackson. It provides a template method {@link #configure()} for custom
 * configurations. The {@link #render(Object)} method returns an empty string if the passed Java object is null. This avoids usually
 * unwanted 404 responses.
 */
public abstract class AbstractJacksonTransformer implements JsonTransformer {

    protected ObjectMapper objectMapper;

    public AbstractJacksonTransformer() {
        this.objectMapper = new ObjectMapper();
        this.configure();
    }

    @Override
    public String render(Object model) {
        // a NULL model means "no content", but we must return an empty String or Spark will assume the call was unsuccessful and
        // create a 404 response
        if (model == null) {
            return "";
        }

        try {
            return this.objectMapper.writeValueAsString(model);
        } catch (JsonProcessingException e) {
            throw new JsonTransformerException("Can't marshal object as JSON string.", e);
        }
    }

    @Override
    public <T> T toObject(String json, Class<T> type) {
        try {
            return this.objectMapper.readValue(json, type);
        } catch (IOException e) {
            throw new JsonTransformerException("Can't unmarshal object from JSON string:\n" + json, e);
        }
    }

    protected abstract void configure();

    protected ObjectMapper getObjectMapper() {
        return this.objectMapper;
    }

    public static class JsonTransformerException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public JsonTransformerException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
