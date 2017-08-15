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

import static java.util.Calendar.MINUTE;
import static java.util.Locale.US;
import static spark.Spark.afterAfter;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;

import spark.Response;

/**
 * A Spark interceptor that counts all requests and provides a statistic via the {@link PublicMetrics} interface.
 */
public class RequestCounterMetrics implements PublicMetrics {

    private static final int STATUS_CODES_COUNT = 5;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int EVERY_MINUTE = 60000;

    private volatile Map<String, AtomicInteger> counter = new ConcurrentHashMap<>(STATUS_CODES_COUNT);
    private Map<Integer, List<Metric<Integer>>> counterRepository = new ConcurrentHashMap<>(MINUTES_PER_HOUR);
    private ReentrantLock lock = new ReentrantLock();

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

    private static String getMetricName(int hour, String id) {
        return new StringBuilder("requests.per_minute.").append(hour < 10 ? "0" : "").append(hour).append(".").append(id).toString();
    }

    @Scheduled(fixedRate = EVERY_MINUTE)
    public void exportMetrics() {
        Map<String, AtomicInteger> lastCounter = this.counter;
        this.counter = new ConcurrentHashMap<>();

        int minute = Calendar.getInstance(TimeZone.getDefault(), US).get(MINUTE);
        int total = 0;
        List<Metric<Integer>> metricsPerMinute = new ArrayList<>();

        for (Entry<String, AtomicInteger> eachCounterEntry : lastCounter.entrySet()) {
            String status = eachCounterEntry.getKey();
            int count = eachCounterEntry.getValue().get();
            total += count;
            metricsPerMinute.add(new Metric<Integer>(getMetricName(minute, status), count));
        }

        metricsPerMinute.add(new Metric<Integer>(getMetricName(minute, "total"), total));
        this.counterRepository.put(minute, metricsPerMinute);
    }

    @Override
    public Collection<Metric<?>> metrics() {
        return this.counterRepository
            .entrySet()
            .stream()
            .flatMap(entry -> entry.getValue().stream())
            .sorted((m1, m2) -> StringUtils.compare(m1.getName(), m2.getName()))
            .collect(Collectors.toList());
    }

    @PostConstruct
    public void mount() {
        afterAfter((req, res) -> this.incrementRequestCount(res));
    }

    private void incrementRequestCount(Response res) {
        String statusKey = getKeyFromStatus(res.status());

        AtomicInteger requestsByStatusCounter = this.counter.get(statusKey);
        if (requestsByStatusCounter == null) {
            try {
                // acquire a lock to avoid double instantiation
                this.lock.lock();

                // check again if the counter is available
                requestsByStatusCounter = this.counter.get(statusKey);

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

        this.counter.put(statusKey, requestsByStatusCounter);
    }
}
