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

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.indoqa.boot.actuate.health.HealthIndicator;
import com.indoqa.boot.actuate.health.SystemHealthIndicator;

@Configuration
public class DefaultHealthActuatorActivator implements ActuatorActivator {

    @Bean
    public Collection<HealthIndicator> getHealthIndicators() {
        Collection<HealthIndicator> healthIndicators = new ArrayList<>();
        healthIndicators.add(new SystemHealthIndicator());
        return healthIndicators;
    }
}
