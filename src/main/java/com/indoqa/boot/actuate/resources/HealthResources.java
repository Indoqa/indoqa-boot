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
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.*;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.indoqa.boot.actuate.health.Health;
import com.indoqa.boot.actuate.health.HealthIndicator;

import spark.Response;
import spark.Spark;

public class HealthResources extends AbstractAdminResources {

    private static final String PATH_HEALTH = "/health";
    private static final int RELOAD_INTERVAL = 5;

    private volatile boolean allUp = false;

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
        // send live health status
        this.getActuator(PATH_HEALTH, (req, res) -> this.sendHealthCheckResult(res));

        // send cached health status
        this.headActuator(PATH_HEALTH, (req, res) -> this.sendHeadHealthCheckResult(res));

        // send cached health status exposed via the business application REST service
        Spark.head(PATH_HEALTH, (req, res) -> this.sendHeadHealthCheckResult(res));
    }

    @PostConstruct
    public void enableReloadHealthStatusTask() {
        new Timer().scheduleAtFixedRate(new HealthCheckerTask(), 0, SECONDS.toMillis(RELOAD_INTERVAL));
    }

    private void setHealthHttpStatus(Response res) {
        res.status(this.allUp ? SC_OK : SC_INTERNAL_SERVER_ERROR);
    }

    private String sendHeadHealthCheckResult(Response res) {
        setHealthHttpStatus(res);
        return EMPTY;
    }

    private ActuatorResults sendHealthCheckResult(Response res) {
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

        this.resetHealthStatus();

        res.type(CONTENT_TYPE_JSON);
        this.setHealthHttpStatus(res);

        return actuatorResults;
    }

    private void resetHealthStatus() {
        HealthResources.this.allUp = HealthResources.this.healthIndicators
            .stream()
            .map(healthIndicator -> healthIndicator.health().getStatus())
            .allMatch(UP::equals);
    }

    protected static class ActuatorResults {

        @JsonIgnore
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

    public class HealthCheckerTask extends TimerTask {

        @Override
        public void run() {
            resetHealthStatus();
        }
    }
}
