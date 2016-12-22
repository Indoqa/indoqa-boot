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
package com.indoqa.boot.profile;

import static com.indoqa.boot.profile.Profile.*;

import java.util.Arrays;

import org.springframework.core.env.Environment;

public final class ProfileDetector {

    private ProfileDetector() {
        // hide utility class constructor
    }

    public static boolean isDev(Environment environment) {
        return isProfileAvailable(environment, DEV);
    }

    public static boolean isProd(Environment environment) {
        return isProfileAvailable(environment, PROD);
    }

    protected static boolean isProfileAvailable(Environment environment, Profile profile) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch(eachProfile -> profile.getName().equals(eachProfile));
    }
}