package com.developer.test.service;

import com.developer.test.dto.ApiMetricsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks simple runtime metrics for the backend in memory.
 * It counts requests, errors, auth failures, and rate-limited calls without extra infrastructure.
 * The metrics controller reads this snapshot and turns it into a clean JSON response.
 */
@Service
public class MetricsService {
    private final long startedAtMillis = System.currentTimeMillis();
    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong successRequests = new AtomicLong();
    private final AtomicLong clientErrors = new AtomicLong();
    private final AtomicLong serverErrors = new AtomicLong();
    private final AtomicLong authFailures = new AtomicLong();
    private final AtomicLong rateLimited = new AtomicLong();
    private final AtomicLong totalDurationMs = new AtomicLong();
    private final AtomicLong maxDurationMs = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> endpointCounts = new ConcurrentHashMap<>();

    public void recordRequest(String method, String path, int status, long durationMs) {
        totalRequests.incrementAndGet();
        totalDurationMs.addAndGet(durationMs);
        endpointCounts.computeIfAbsent(method + " " + path, key -> new AtomicLong()).incrementAndGet();

        updateMaxDuration(durationMs);

        if (status >= 500) {
            serverErrors.incrementAndGet();
        } else if (status >= 400) {
            clientErrors.incrementAndGet();
        } else {
            successRequests.incrementAndGet();
        }
    }

    public ApiMetricsResponse snapshot(boolean apiKeyRequired) {
        ApiMetricsResponse response = new ApiMetricsResponse();
        response.setTimestamp(Instant.now());
        response.setUptimeSeconds((System.currentTimeMillis() - startedAtMillis) / 1000L);

        ApiMetricsResponse.RequestMetrics requestMetrics = new ApiMetricsResponse.RequestMetrics();
        requestMetrics.setTotal(totalRequests.get());
        requestMetrics.setSuccess(successRequests.get());
        requestMetrics.setClientErrors(clientErrors.get());
        requestMetrics.setServerErrors(serverErrors.get());
        response.setRequests(requestMetrics);

        ApiMetricsResponse.SecurityMetrics securityMetrics = new ApiMetricsResponse.SecurityMetrics();
        securityMetrics.setApiKeyRequired(apiKeyRequired);
        securityMetrics.setAuthFailures(authFailures.get());
        securityMetrics.setRateLimited(rateLimited.get());
        response.setSecurity(securityMetrics);

        ApiMetricsResponse.PerformanceMetrics performanceMetrics = new ApiMetricsResponse.PerformanceMetrics();
        performanceMetrics.setAverageDurationMs(totalRequests.get() == 0 ? 0 : totalDurationMs.get() / totalRequests.get());
        performanceMetrics.setMaxDurationMs(maxDurationMs.get());
        response.setPerformance(performanceMetrics);

        response.setEndpoints(snapshotEndpointCounts());
        return response;
    }

    private void updateMaxDuration(long durationMs) {
        long current = maxDurationMs.get();
        while (durationMs > current && !maxDurationMs.compareAndSet(current, durationMs)) {
            current = maxDurationMs.get();
        }
    }

    private Map<String, Long> snapshotEndpointCounts() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(endpointCounts.entrySet());
        entries.sort(Comparator.comparingLong((Map.Entry<String, AtomicLong> entry) -> entry.getValue().get()).reversed());
        for (Map.Entry<String, AtomicLong> entry : entries) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return snapshot;
    }

    public void recordAuthFailure() {
        authFailures.incrementAndGet();
    }

    public void recordRateLimited() {
        rateLimited.incrementAndGet();
    }
}
