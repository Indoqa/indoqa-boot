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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.indoqa.boot.html.react.AbstractReactResourceBase;

/**
 * <p>
 * The Indoqa Boot startup process has several callback methods to hook into the startup process. You can implement them to run code at
 * particular times in this process. Methods prefixed with will are called right before something happens, and methods prefixed with
 * did are called right after something happens.
 * </p>
 * <p>
 * This is the execution order of these lifecycle methods:
 * </p>
 * <ol>
 * <li>{@link #willInitialize()}</li>
 * <li>{@link #willCreateSpringContext()}</li>
 * <li>{@link #didCreateSpringContext(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #willCreateDefaultSparkRoutes(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #willScanForComponents(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #willRefreshSpringContext(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #didInitializeSpring(AnnotationConfigApplicationContext)}</li>
 * <li>{@link #didInitialize()}</li>
 * </ol>
 */
public interface StartupLifecycle {

    /**
     * Use this method to operate on the uninitialized Spring application context. It is called after the Spring
     * {@link ApplicationContext} is created.
     * 
     * @param The Spring application context.
     */
    void didCreateSpringContext(AnnotationConfigApplicationContext context);

    /**
     * Use this method to provide additional information to the the startup status message.
     * 
     * @return Additional startup status message.
     */
    Optional<CharSequence> didInitialize();

    /**
     * Use this method for operations that need a completely initialized Spring.
     * 
     * @param The Spring application context.
     */
    void didInitializeSpring(AnnotationConfigApplicationContext context);

    /**
     * Use this method to perform Spark operations that have to happen before any routes are registered. E.g. extensions of the
     * {@link AbstractReactResourceBase} have to be registered here.
     * 
     * @param The Spring application context.
     */
    void willCreateDefaultSparkRoutes(AnnotationConfigApplicationContext context);

    /**
     * Use this method for preparations for Spring or Spark. {@link #willCreateSpringContext()} is called before Spring and Spark
     * initialized and after the startup message is printed to the console.
     */
    void willCreateSpringContext();

    /**
     * Use this method to setup variables that should be globally available and maybe printed to the console. It is called before
     * Spring and Spark are initialized and the startup message is printed to the console.
     */
    void willInitialize();

    /**
     * Use this method to access the Spring application context before it will be refreshed.
     * 
     * @param The Spring application context.
     */
    void willRefreshSpringContext(AnnotationConfigApplicationContext context);

    /**
     * Use this method to perform operations before the Spring component scan is invoked.
     * 
     * @param The Spring application context.
     */
    void willScanForComponents(AnnotationConfigApplicationContext context);

}
