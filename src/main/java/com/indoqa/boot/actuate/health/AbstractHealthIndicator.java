/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indoqa.boot.actuate.health;

import static org.slf4j.LoggerFactory.getLogger;

import com.indoqa.boot.actuate.health.Health.Builder;
import org.slf4j.Logger;

/**
 * Base {@link HealthIndicator} implementations that encapsulates creation of {@link Health} instance and error handling.
 * <p>
 * This implementation is only suitable if an {@link Exception} raised from
 * {@link #doHealthCheck(org.springframework.boot.actuate.health.Health.Builder)} should create a {@link Status#DOWN} health status.
 */
public abstract class AbstractHealthIndicator implements HealthIndicator {

    private final Logger logger = getLogger(this.getClass());

    @Override
    public final Health health() {
        Builder builder = new Builder();
        try {
            this.doHealthCheck(builder);
        } catch (Exception ex) {
            this.logger.warn("Health check failed", ex);
            builder.down(ex);
        }
        return builder.build();
    }

    /**
     * Actual health check logic.
     * 
     * @param builder the {@link Builder} to report health status and details
     * @throws Exception any {@link Exception} that should create a {@link Status#DOWN} system status.
     */
    protected abstract void doHealthCheck(Builder builder) throws Exception;

}
