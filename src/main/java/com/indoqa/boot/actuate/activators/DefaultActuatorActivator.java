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
package com.indoqa.boot.actuate.activators;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.indoqa.boot.actuate.health.SystemHealthIndicator;
import com.indoqa.boot.actuate.metrics.RequestCounterMetrics;
import com.indoqa.boot.actuate.metrics.SystemPublicMetrics;

@Configuration
@EnableScheduling
public class DefaultActuatorActivator implements ActuatorActivator {

    @Bean
    public RequestCounterMetrics getRequestCounterMetrics() {
        return new RequestCounterMetrics();
    }

    @Bean
    public SystemHealthIndicator getSystemHealthIndicator() {
        return new SystemHealthIndicator();
    }

    @Bean
    public SystemPublicMetrics getSystemPublicMetrics() {
        return new SystemPublicMetrics();
    }
}
