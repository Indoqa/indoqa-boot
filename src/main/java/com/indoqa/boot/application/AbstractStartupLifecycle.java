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
package com.indoqa.boot.application;

import java.util.Optional;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Extend this class to provide custom callback methods that are executed during the application startup.
 */
public abstract class AbstractStartupLifecycle implements StartupLifecycle {

    @Override
    public void didCreateSpringContext(AnnotationConfigApplicationContext context) {
        // empty implementation
    }

    @Override
    public Optional<CharSequence> didInitialize() {
        return Optional.empty();
    }

    @Override
    public void didInitializeSpring(AnnotationConfigApplicationContext context) {
        // empty implementation
    }

    @Override
    public void willCreateDefaultSparkRoutes(AnnotationConfigApplicationContext context) {
        // empty implementation
    }

    @Override
    public void willCreateSpringContext() {
        // empty implementation
    }

    @Override
    public void willInitialize() {
        // empty implementation
    }

    @Override
    public void willRefreshSpringContext(AnnotationConfigApplicationContext context) {
        // empty implementation
    }

    @Override
    public void willScanForComponents(AnnotationConfigApplicationContext context) {
        // empty implementation
    }
}
