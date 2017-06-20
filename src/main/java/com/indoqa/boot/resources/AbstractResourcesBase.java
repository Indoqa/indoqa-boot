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
package com.indoqa.boot.resources;

import static org.apache.commons.lang3.StringUtils.*;

import javax.annotation.PostConstruct;

import com.indoqa.boot.ApplicationInitializationException;

public abstract class AbstractResourcesBase {

    private static final String DEFAULT_BASE_PATH = "";

    public AbstractResourcesBase() {
        super();
    }

    @PostConstruct
    public void checkResourceBase() {
        CharSequence resourceBase = this.getResourceBase();
        if (isBlank(resourceBase)) {
            return;
        }

        if (!startsWith(resourceBase, "/")) {
            throw new ApplicationInitializationException(
                "The Spark resource base path '" + resourceBase + "' is invalid. It must start with a '/'.");
        }

        if (endsWith(resourceBase, "/")) {
            throw new ApplicationInitializationException(
                "The Spark resource base path '" + resourceBase + "' is invalid. It must not end with a '/'.");
        }
    }

    protected CharSequence getResourceBase() {
        return DEFAULT_BASE_PATH;
    }

    protected String resolvePath(CharSequence path) {
        if (!startsWith(path, "/")) {
            throw new ApplicationInitializationException(
                "A Spark resource cannot be mounted to '" + path + "'. The path has to start with a '/'.");
        }

        return new StringBuilder(this.getResourceBase()).append(path).toString();
    }
}
