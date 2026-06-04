package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Holds the runtime metrics snapshot returned by the metrics endpoint.
 * It includes request counts, security events, timing data, and per-endpoint totals.
 * The service builds this response so the controller can return it without extra logic.
 */
public class ApiMetricsResponse {
    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("uptimeSeconds")
    private long uptimeSeconds;

    @JsonProperty("requests")
    private RequestMetrics requests;

    @JsonProperty("security")
    private SecurityMetrics security;

    @JsonProperty("performance")
    private PerformanceMetrics performance;

    @JsonProperty("endpoints")
    private Map<String, Long> endpoints;

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public long getUptimeSeconds() {
        return uptimeSeconds;
    }

    public void setUptimeSeconds(long uptimeSeconds) {
        this.uptimeSeconds = uptimeSeconds;
    }

    public RequestMetrics getRequests() {
        return requests;
    }

    public void setRequests(RequestMetrics requests) {
        this.requests = requests;
    }

    public SecurityMetrics getSecurity() {
        return security;
    }

    public void setSecurity(SecurityMetrics security) {
        this.security = security;
    }

    public PerformanceMetrics getPerformance() {
        return performance;
    }

    public void setPerformance(PerformanceMetrics performance) {
        this.performance = performance;
    }

    public Map<String, Long> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, Long> endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * Counts requests and groups them by outcome.
     * This gives a fast snapshot of the traffic handled by the API.
     * It is simple enough to inspect without special tooling.
     */
    public static class RequestMetrics {
        @JsonProperty("total")
        private long total;

        @JsonProperty("success")
        private long success;

        @JsonProperty("clientErrors")
        private long clientErrors;

        @JsonProperty("serverErrors")
        private long serverErrors;

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getSuccess() {
            return success;
        }

        public void setSuccess(long success) {
            this.success = success;
        }

        public long getClientErrors() {
            return clientErrors;
        }

        public void setClientErrors(long clientErrors) {
            this.clientErrors = clientErrors;
        }

        public long getServerErrors() {
            return serverErrors;
        }

        public void setServerErrors(long serverErrors) {
            this.serverErrors = serverErrors;
        }
    }

    /**
     * Tracks the security events this API cares about.
     * It shows whether API key protection is enabled and how often requests were blocked.
     * That makes the security state easy to explain from one response.
     */
    public static class SecurityMetrics {
        @JsonProperty("apiKeyRequired")
        private boolean apiKeyRequired;

        @JsonProperty("authFailures")
        private long authFailures;

        @JsonProperty("rateLimited")
        private long rateLimited;

        public boolean isApiKeyRequired() {
            return apiKeyRequired;
        }

        public void setApiKeyRequired(boolean apiKeyRequired) {
            this.apiKeyRequired = apiKeyRequired;
        }

        public long getAuthFailures() {
            return authFailures;
        }

        public void setAuthFailures(long authFailures) {
            this.authFailures = authFailures;
        }

        public long getRateLimited() {
            return rateLimited;
        }

        public void setRateLimited(long rateLimited) {
            this.rateLimited = rateLimited;
        }
    }

    /**
     * Tracks the basic timing numbers for requests.
     * It includes the average duration and the slowest request seen so far.
     * This gives a simple view of performance without extra infrastructure.
     */
    public static class PerformanceMetrics {
        @JsonProperty("averageDurationMs")
        private long averageDurationMs;

        @JsonProperty("maxDurationMs")
        private long maxDurationMs;

        public long getAverageDurationMs() {
            return averageDurationMs;
        }

        public void setAverageDurationMs(long averageDurationMs) {
            this.averageDurationMs = averageDurationMs;
        }

        public long getMaxDurationMs() {
            return maxDurationMs;
        }

        public void setMaxDurationMs(long maxDurationMs) {
            this.maxDurationMs = maxDurationMs;
        }
    }
}
