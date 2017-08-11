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
package com.indoqa.boot.actuate.systeminfo;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"value", "source", "overridden"})
public class SpringProperty {

    private final String name;
    private final List<SpringPropertyValue> values = new ArrayList<>();

    public SpringProperty(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getName() {
        return this.name;
    }

    @JsonInclude(NON_EMPTY)
    public List<SpringPropertyValue> getOverridden() {
        return this.values.stream().filter(prop -> !prop.equals(this.getFirstValue())).collect(toList());
    }

    public String getSource() {
        return this.getFirstValue().getSource();
    }

    public Object getValue() {
        return this.getFirstValue().getValue();
    }

    @JsonIgnore
    public boolean hasOverriddenProperties() {
        return !this.getOverridden().isEmpty();
    }

    public void setValue(Object value, String source) {
        this.values.add(new SpringPropertyValue(value, source));
    }

    private SpringPropertyValue getFirstValue() {
        return this.values.get(0);
    }

    @JsonPropertyOrder({"value", "source"})
    public static class SpringPropertyValue {

        private final Object value;
        private final String source;

        public SpringPropertyValue(Object value, String source) {
            this.value = value;
            this.source = source;
        }

        public String getSource() {
            return this.source;
        }

        public Object getValue() {
            return this.value;
        }
    }
}
