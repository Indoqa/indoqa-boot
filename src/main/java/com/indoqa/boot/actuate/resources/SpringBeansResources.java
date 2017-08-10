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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.LiveBeansView;
import org.springframework.util.Assert;

public class SpringBeansResources extends AbstractActuatorResources implements ApplicationContextAware {

    private final HierarchyAwareLiveBeansView liveBeansView = new HierarchyAwareLiveBeansView();

    @PostConstruct
    public void mount() {
        this.get("/spring-beans", (req, res) -> this.getSpringBeansLiveView());
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (context.getEnvironment().getProperty(LiveBeansView.MBEAN_DOMAIN_PROPERTY_NAME) == null) {
            this.liveBeansView.setLeafContext(context);
        }
    }

    private String getSpringBeansLiveView() {
        return this.liveBeansView.getSnapshotAsJson();
    }

    private static class HierarchyAwareLiveBeansView extends LiveBeansView {

        private ConfigurableApplicationContext leafContext;

        private static ConfigurableApplicationContext asConfigurableContext(ApplicationContext applicationContext) {
            Assert.isTrue(
                applicationContext instanceof ConfigurableApplicationContext,
                "'" + applicationContext + "' does not implement ConfigurableApplicationContext");
            return (ConfigurableApplicationContext) applicationContext;
        }

        @Override
        public String getSnapshotAsJson() {
            if (this.leafContext == null) {
                return super.getSnapshotAsJson();
            }
            return this.generateJson(this.getContextHierarchy());
        }

        private Set<ConfigurableApplicationContext> getContextHierarchy() {
            Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();
            ApplicationContext context = this.leafContext;
            while (context != null) {
                contexts.add(asConfigurableContext(context));
                context = context.getParent();
            }
            return contexts;
        }

        private void setLeafContext(ApplicationContext leafContext) {
            this.leafContext = asConfigurableContext(leafContext);
        }
    }
}
