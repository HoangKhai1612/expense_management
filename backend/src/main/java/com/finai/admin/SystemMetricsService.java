package com.finai.admin;

import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-local runtime metrics for the admin monitoring screen.
 *
 * Counters are explicitly scoped to the running process and reset on restart.
 * They are labelled as such in the API response so the admin UI can show the
 * window they cover rather than presenting them as all-time figures.
 */
@Service
public class SystemMetricsService {

    private final AtomicLong unhandledErrors = new AtomicLong();
    private final Instant startedAt = Instant.now();

    public void recordUnhandledError() {
        unhandledErrors.incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startedAt", startedAt);
        result.put("uptimeSeconds", Duration.between(startedAt, Instant.now()).toSeconds());
        result.put("uptimeSinceRestart", true);
        result.put("unhandledErrorsSinceStartup", unhandledErrors.get());
        result.put("jvmName", ManagementFactory.getRuntimeMXBean().getName());
        result.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        result.put("maxMemoryBytes", Runtime.getRuntime().maxMemory());

        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        result.put("usedMemoryBytes", used);
        result.put("memoryUsagePercentage",
                (int) Math.round(used * 100.0 / runtime.maxMemory()));
        return result;
    }
}
