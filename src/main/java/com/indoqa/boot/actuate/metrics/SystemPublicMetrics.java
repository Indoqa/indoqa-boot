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
package com.indoqa.boot.actuate.metrics;

import static java.lang.System.currentTimeMillis;
import static java.util.Locale.US;

import java.lang.management.*;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * A {@link PublicMetrics} implementation that provides various system-related metrics.
 */
public class SystemPublicMetrics implements PublicMetrics {

    private static final int BYTES_TO_KBYTES = 1024;
    private long timestamp;

    public SystemPublicMetrics() {
        this.timestamp = currentTimeMillis();
    }

    /**
     * Add JVM non-heap metrics.
     * 
     * @param result the result
     */
    private static void addNonHeapMetrics(Collection<Metric<?>> result) {
        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
        result.add(newMemoryMetric("nonheap.committed", memoryUsage.getCommitted()));
        result.add(newMemoryMetric("nonheap.init", memoryUsage.getInit()));
        result.add(newMemoryMetric("nonheap.used", memoryUsage.getUsed()));
        result.add(newMemoryMetric("nonheap", memoryUsage.getMax()));
    }

    /**
     * Turn GC names like 'PS Scavenge' or 'PS MarkSweep' into something that is more metrics friendly.
     * 
     * @param name the source name
     * @return a metric friendly name
     */
    private static String beautifyGcName(String name) {
        return StringUtils.replace(name, " ", "_").toLowerCase(US);
    }

    private static long getTotalNonHeapMemoryIfPossible() {
        try {
            return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
        } catch (Throwable ex) {
            return 0;
        }
    }

    private static Metric<Long> newMemoryMetric(String name, long bytes) {
        return new Metric<>(name, bytes / BYTES_TO_KBYTES);
    }

    @Override
    public Collection<Metric<?>> metrics() {
        Collection<Metric<?>> result = new LinkedHashSet<>();
        this.addBasicMetrics(result);
        this.addManagementMetrics(result);
        return result;
    }

    /**
     * Add basic system metrics.
     * 
     * @param result the result
     */
    protected void addBasicMetrics(Collection<Metric<?>> result) {
        // NOTE: ManagementFactory must not be used here since it fails on GAE
        Runtime runtime = Runtime.getRuntime();
        result.add(newMemoryMetric("mem", runtime.totalMemory() + getTotalNonHeapMemoryIfPossible()));
        result.add(newMemoryMetric("mem.free", runtime.freeMemory()));
        result.add(new Metric<>("processors", runtime.availableProcessors()));
        result.add(new Metric<>("instance.uptime", System.currentTimeMillis() - this.timestamp));
    }

    /**
     * Add class loading metrics.
     * 
     * @param result the result
     */
    protected void addClassLoadingMetrics(Collection<Metric<?>> result) {
        ClassLoadingMXBean classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
        result.add(new Metric<>("classes", (long) classLoadingMxBean.getLoadedClassCount()));
        result.add(new Metric<>("classes.loaded", classLoadingMxBean.getTotalLoadedClassCount()));
        result.add(new Metric<>("classes.unloaded", classLoadingMxBean.getUnloadedClassCount()));
    }

    /**
     * Add garbage collection metrics.
     * 
     * @param result the result
     */
    protected void addGarbageCollectionMetrics(Collection<Metric<?>> result) {
        List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMxBeans) {
            String name = beautifyGcName(garbageCollectorMXBean.getName());
            result.add(new Metric<>("gc." + name + ".count", garbageCollectorMXBean.getCollectionCount()));
            result.add(new Metric<>("gc." + name + ".time", garbageCollectorMXBean.getCollectionTime()));
        }
    }

    /**
     * Add JVM heap metrics.
     * 
     * @param result the result
     */
    protected void addHeapMetrics(Collection<Metric<?>> result) {
        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        result.add(newMemoryMetric("heap.committed", memoryUsage.getCommitted()));
        result.add(newMemoryMetric("heap.init", memoryUsage.getInit()));
        result.add(newMemoryMetric("heap.used", memoryUsage.getUsed()));
        result.add(newMemoryMetric("heap", memoryUsage.getMax()));
    }

    /**
     * Add thread metrics.
     * 
     * @param result the result
     */
    protected void addThreadMetrics(Collection<Metric<?>> result) {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        result.add(new Metric<>("threads.peak", (long) threadMxBean.getPeakThreadCount()));
        result.add(new Metric<>("threads.daemon", (long) threadMxBean.getDaemonThreadCount()));
        result.add(new Metric<>("threads.totalStarted", threadMxBean.getTotalStartedThreadCount()));
        result.add(new Metric<>("threads", (long) threadMxBean.getThreadCount()));
    }

    /**
     * Add metrics from ManagementFactory if possible. Note that ManagementFactory is not available on Google App Engine.
     * 
     * @param result the result
     */
    private void addManagementMetrics(Collection<Metric<?>> result) {
        try {
            // Add JVM up time in ms
            result.add(new Metric<>("uptime", ManagementFactory.getRuntimeMXBean().getUptime()));
            result.add(new Metric<>("systemload.average", ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
            this.addHeapMetrics(result);
            addNonHeapMetrics(result);
            this.addThreadMetrics(result);
            this.addClassLoadingMetrics(result);
            this.addGarbageCollectionMetrics(result);
        } catch (NoClassDefFoundError ex) {
            // Expected on Google App Engine
        }
    }
}
