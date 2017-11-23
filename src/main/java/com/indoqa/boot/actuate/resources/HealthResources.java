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

import static com.indoqa.boot.actuate.health.Status.UP;
import static java.util.Locale.US;
import static java.util.stream.Collectors.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.indoqa.boot.actuate.health.Health;
import com.indoqa.boot.actuate.health.HealthIndicator;

import spark.Response;

public class HealthResources extends AbstractAdminResources {

    private static final String PATH_HEALTH = "/health";

    @Inject
    private Collection<HealthIndicator> healthIndicators;

    private static String getKey(String name) {
        int index = name.toLowerCase(US).indexOf("healthindicator");
        if (index > 0) {
            return name.substring(0, index).toLowerCase(US);
        }
        return name;
    }

    @PostConstruct
    public void mount() {
        this.getActuator(PATH_HEALTH, (req, res) -> this.getHealthCheckResult(res));
        this.headActuator(PATH_HEALTH, (req, res) -> this.headHealthCheckResult(res));
    }

    private void setHealthHttpStatus(Response res) {
        boolean allUp = this.healthIndicators
            .stream()
            .map(healthIndicator -> healthIndicator.health().getStatus())
            .allMatch(UP::equals);

        res.status(allUp ? SC_OK : SC_INTERNAL_SERVER_ERROR);
    }

    private Object headHealthCheckResult(Response res) {
        setHealthHttpStatus(res);
        return EMPTY;
    }

    private ActuatorResults getHealthCheckResult(Response res) {
        Map<String, List<HealthIndicator>> groupedHealthIndicators = this.healthIndicators
            .stream()
            .collect(groupingBy(healthIndicator -> getKey(healthIndicator.getClass().getSimpleName()), toList()));

        ActuatorResults actuatorResults = new ActuatorResults();

        for (Entry<String, List<HealthIndicator>> eachHealthIndicatorEntry : groupedHealthIndicators.entrySet()) {
            List<HealthIndicator> healthIndicators = eachHealthIndicatorEntry.getValue();

            if (healthIndicators.isEmpty()) {
                continue;
            }

            if (healthIndicators.size() == 1) {
                actuatorResults.add(eachHealthIndicatorEntry.getKey(), healthIndicators.get(0).health());
            }

            else {
                List<Health> healths = healthIndicators.stream().map(HealthIndicator::health).collect(toList());
                actuatorResults.add(eachHealthIndicatorEntry.getKey(), healths);
            }
        }

        this.setHealthHttpStatus(res);

        return actuatorResults;
    }

    protected static class ActuatorResults {

        private final Map<String, Object> results = new HashMap<>();

        public void add(String key, Collection<Health> healths) {
            this.results.put(key, healths);
        }

        public void add(String key, Health health) {
            this.results.put(key, health);
        }

        @JsonAnyGetter
        public Map<String, Object> getResults() {
            return this.results;
        }
    }
}
