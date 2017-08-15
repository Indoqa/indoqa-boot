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

import static java.util.Calendar.*;
import static java.util.Locale.US;
import static spark.Spark.afterAfter;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

import spark.Response;

/**
 * A Spark interceptor that counts all requests and provides statistics via the {@link PublicMetrics} interface.
 */
public class RequestCounterMetrics implements PublicMetrics {

    private static final String METRIC_MINUTE = "minute";
    private static final String METRIC_HOUR = "hour";
    private static final String METRIC_TOTAL = "total";

    private static final String METRIC_PREFIX_PER_MINUTE = "requests.per_minute.";
    private static final String METRIC_PREFIX_PER_HOUR = "requests.per_hour.";
    private static final String METRIC_PREFIX_CURRENT_TIME = "requests.current_time.";

    private static final int STATUS_CODES_COUNT = 5;
    private static final int CURRENT_TIME_METRICS_COUNT = 2;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int HOURS_PER_DAY = 24;
    private static final int EVERY_MINUTE = 60000;

    private ReentrantLock lock = new ReentrantLock();
    private boolean firstFullHourReached;

    private volatile Map<String, AtomicInteger> activeMinutesCounter = new ConcurrentHashMap<>(STATUS_CODES_COUNT);
    private Map<String, Integer> activeHourCounter = new ConcurrentHashMap<>(HOURS_PER_DAY);

    private Map<Integer, List<Metric<Integer>>> minuteMetricsRepository = new ConcurrentHashMap<>(MINUTES_PER_HOUR);
    private Map<Integer, List<Metric<Integer>>> hourMetricsRepository = new ConcurrentHashMap<>(HOURS_PER_DAY);
    private Map<String, Metric<Integer>> currentTimeMetricsRepository = new ConcurrentHashMap<>(CURRENT_TIME_METRICS_COUNT);

    private static int calcPreviousHour(int currentHour) {
        if (currentHour == 0) {
            return HOURS_PER_DAY - 1;
        }
        return currentHour - 1;
    }

    private static int calcPreviousMinute(int currentMinute) {
        if (currentMinute == 0) {
            return MINUTES_PER_HOUR - 1;
        }
        return currentMinute - 1;
    }

    private static List<Metric<Integer>> createHourMetrics(Map<String, Integer> activeHourCounter, int currentHour) {
        List<Metric<Integer>> hourMetrics = new ArrayList<>();
        int hourTotal = 0;

        // add status metrics
        for (Entry<String, Integer> eachHourStatusEntry : activeHourCounter.entrySet()) {
            String status = eachHourStatusEntry.getKey();
            Integer value = eachHourStatusEntry.getValue();
            hourMetrics.add(new Metric<>(getMetricPerHourName(currentHour, status), value));
            hourTotal += value;
        }

        // add total metric
        hourMetrics.add(new Metric<>(getMetricPerHourName(currentHour, METRIC_TOTAL), hourTotal));
        return hourMetrics;
    }

    private static List<Metric<Integer>> createMinuteMetrics(Map<String, AtomicInteger> lastCounter, int currentMinute) {
        List<Metric<Integer>> metricsPerMinute = new ArrayList<>(STATUS_CODES_COUNT);
        int total = 0;

        for (Entry<String, AtomicInteger> eachCounterEntry : lastCounter.entrySet()) {
            String status = eachCounterEntry.getKey();
            int count = eachCounterEntry.getValue().get();
            total += count;

            Metric<Integer> minuteStatusMetric = new Metric<Integer>(getMetricPerMinuteName(currentMinute, status), count);
            metricsPerMinute.add(minuteStatusMetric);
        }

        // add total metric
        metricsPerMinute.add(new Metric<Integer>(getMetricPerMinuteName(currentMinute, METRIC_TOTAL), total));
        return metricsPerMinute;
    }

    private static String createTimeMetricName(String id) {
        return METRIC_PREFIX_CURRENT_TIME + id;
    }

    private static String getKeyFromStatus(int status) {
        if (status < 200) {
            return "1xx";
        }
        if (status < 300) {
            return "2xx";
        }
        if (status < 400) {
            return "3xx";
        }
        if (status < 500) {
            return "4xx";
        }
        return "5xx";
    }

    private static String getMetricName(String prefix, int nr, String id) {
        return new StringBuilder(prefix).append(nr < 10 ? "0" : "").append(nr).append(".").append(id).toString();
    }

    private static String getMetricPerHourName(int hour, String id) {
        return getMetricName(METRIC_PREFIX_PER_HOUR, hour, id);
    }

    private static String getMetricPerMinuteName(int minute, String id) {
        return getMetricName(METRIC_PREFIX_PER_MINUTE, minute, id);
    }

    @Scheduled(fixedRate = EVERY_MINUTE)
    public void exportMetrics() {
        Map<String, AtomicInteger> lastCounter = this.resetMinutesCounter();

        Calendar now = Calendar.getInstance(TimeZone.getDefault(), US);
        int currentMinute = now.get(MINUTE);
        int currentHour = now.get(HOUR_OF_DAY);
        int previousMinute = calcPreviousMinute(currentMinute);
        int previousHour = calcPreviousHour(currentHour);

        this.exportHourMetrics(previousMinute, previousHour);
        this.exportMinuteMetrics(lastCounter, previousMinute);
        this.incrementActiveHourCounter(lastCounter);
        this.exportCurrentTimeMetrics(currentMinute, currentHour);
    }

    @Override
    public Collection<Metric<?>> metrics() {
        Stream<Metric<Integer>> counterMetricsStream = Stream
            .concat(this.minuteMetricsRepository.entrySet().stream(), this.hourMetricsRepository.entrySet().stream())
            .flatMap(entry -> entry.getValue().stream());

        Stream<Metric<Integer>> currentTimeMetricsStream = this.currentTimeMetricsRepository.values().stream();

        return Stream
            .concat(counterMetricsStream, currentTimeMetricsStream)
            .sorted((m1, m2) -> StringUtils.compare(m1.getName(), m2.getName()))
            .collect(Collectors.toList());
    }

    @PostConstruct
    public void mount() {
        afterAfter((req, res) -> this.incrementRequestCount(res));
    }

    private void exportCurrentTimeMetrics(int currentMinute, int currentHour) {
        this.currentTimeMetricsRepository.put(METRIC_HOUR, new Metric<>(createTimeMetricName(METRIC_HOUR), currentHour));
        this.currentTimeMetricsRepository.put(METRIC_MINUTE, new Metric<>(createTimeMetricName(METRIC_MINUTE), currentMinute));
    }

    private void exportHourMetrics(int previousMinute, int currentHour) {
        if (previousMinute != 0) {
            return;
        }

        this.firstFullHourReached = true;

        // as long as there are no status metrics collected, do not perform an export
        if (this.activeHourCounter.isEmpty()) {
            return;
        }

        this.hourMetricsRepository.put(currentHour, createHourMetrics(this.activeHourCounter, currentHour));
        this.activeHourCounter.clear();
    }

    private void exportMinuteMetrics(Map<String, AtomicInteger> lastCounter, int currentMinute) {
        this.minuteMetricsRepository.put(currentMinute, createMinuteMetrics(lastCounter, currentMinute));
    }

    private void incrementActiveHourCounter(Map<String, AtomicInteger> lastCounter) {
        if (!this.firstFullHourReached) {
            return;
        }

        for (Entry<String, AtomicInteger> eachCounterEntry : lastCounter.entrySet()) {
            String status = eachCounterEntry.getKey();
            int nextCount = eachCounterEntry.getValue().get();
            Integer currentHourCount = this.activeHourCounter.getOrDefault(status, 0);
            this.activeHourCounter.put(status, currentHourCount + nextCount);
        }
    }

    private void incrementRequestCount(Response res) {
        String statusKey = getKeyFromStatus(res.status());

        AtomicInteger requestsByStatusCounter = this.activeMinutesCounter.get(statusKey);
        if (requestsByStatusCounter == null) {
            try {
                // acquire a lock to avoid double instantiation of the counter
                this.lock.lock();

                // check again if the activeMinutesCounter is available
                requestsByStatusCounter = this.activeMinutesCounter.get(statusKey);

                // if not, create a new one under the protection of the lock
                if (requestsByStatusCounter == null) {
                    requestsByStatusCounter = new AtomicInteger();
                }
            } finally {
                // release the lock
                this.lock.unlock();
            }
        }

        requestsByStatusCounter.incrementAndGet();

        this.activeMinutesCounter.put(statusKey, requestsByStatusCounter);
    }

    private Map<String, AtomicInteger> resetMinutesCounter() {
        Map<String, AtomicInteger> lastCounter = this.activeMinutesCounter;
        this.activeMinutesCounter = new ConcurrentHashMap<>();
        return lastCounter;
    }
}
