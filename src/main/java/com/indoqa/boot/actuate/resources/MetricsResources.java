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
package com.indoqa.boot.actuate.resources;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.actuate.metrics.Metric;
import com.indoqa.boot.actuate.metrics.PublicMetrics;

public class MetricsResources extends AbstractActuatorResources {

    @Inject
    private Collection<PublicMetrics> publicMetrics;

    public Map<String, Object> getMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<PublicMetrics> metrics = new ArrayList<>(this.publicMetrics);
        for (PublicMetrics publicMetric : metrics) {
            try {
                for (Metric<?> metric : publicMetric.metrics()) {
                    result.put(metric.getName(), metric.getValue());
                }
            } catch (Exception ex) {
                // Could not evaluate metrics
            }
        }
        return result;
    }

    @PostConstruct
    public void mount() {
        this.getActuator("/metrics", (req, res) -> this.getMetrics());
    }
}
