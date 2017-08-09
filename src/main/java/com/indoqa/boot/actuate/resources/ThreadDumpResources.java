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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.indoqa.boot.json.resources.AbstractJsonResourcesBase;
import com.indoqa.boot.json.transformer.JsonTransformer;
import com.indoqa.boot.spark.SparkAdminService;

public class ThreadDumpResources extends AbstractJsonResourcesBase {

    @Inject
    private SparkAdminService sparkAdminService;

    @Inject
    private JsonTransformer jsonTransformer;

    private static List<ThreadInfo> getThreadDump() {
        return Arrays.asList(ManagementFactory.getThreadMXBean().dumpAllThreads(true, true));
    }

    @PostConstruct
    public void mount() {
        if (this.sparkAdminService.isAvailable()) {
            this.sparkAdminService.instance().get("/dump", (req, res) -> getThreadDump(), this.jsonTransformer);
        } else {
            this.get("/dump", (req, res) -> getThreadDump());
        }
    }
}
