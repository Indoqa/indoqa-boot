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
package com.indoqa.boot.lifecycle;

import java.util.Optional;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * <p>
 * The Indoqa Boot startup-process has server "lifecycle methods". You can implement them to run code at particular times in this
 * process. Methods prefixed with will are called right before something happens, and methods prefixed with did are called right after
 * something happens.
 * </p>
 * <p>
 * The execution order of the lifecycle methods:
 * </p>
 * <ul>
 * <li>{@link #willInitialize()}</li>
 * <li>{@link #willCreateSpringContext()}</li>
 * <li>{@link #didCreateSpringContext(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #willCreateDefaultSparkRoutes()}</li>
 * <li>{@link #willScanForComponents()}</li>
 * <li>{@link #willRefreshSpringContext()}</li>
 * <li>{@link #didInitializeSpring()}</li>
 * <li>{@link #didInitialize(StringBuilder)}</li>
 * </ul>
 */
public interface StartupLifecycle {

    void didCreateSpringContext(AnnotationConfigApplicationContext context);

    Optional<CharSequence> didInitialize();

    void didInitializeSpring();

    void willCreateDefaultSparkRoutes();

    void willCreateSpringContext();

    void willInitialize();

    void willRefreshSpringContext();

    void willScanForComponents();

}
