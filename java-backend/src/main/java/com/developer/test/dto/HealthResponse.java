package com.developer.test.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Carries the full health snapshot for the backend.
 * It combines service status, cache state, storage state, and data counts.
 * The health endpoint uses it so monitoring tools get one clear response.
 */
public class HealthResponse {
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("version")
    private String version;

    @JsonProperty("storage")
    private ComponentStatus storage;

    @JsonProperty("dataSource")
    private ComponentStatus dataSource;

    @JsonProperty("cache")
    private CacheStatus cache;

    @JsonProperty("data")
    private DataStatus data;

    public HealthResponse() {
    }

    public HealthResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ComponentStatus getStorage() {
        return storage;
    }

    public void setStorage(ComponentStatus storage) {
        this.storage = storage;
    }

    public ComponentStatus getDataSource() {
        return dataSource;
    }

    public void setDataSource(ComponentStatus dataSource) {
        this.dataSource = dataSource;
    }

    public CacheStatus getCache() {
        return cache;
    }

    public void setCache(CacheStatus cache) {
        this.cache = cache;
    }

    public DataStatus getData() {
        return data;
    }

    public void setData(DataStatus data) {
        this.data = data;
    }

    /**
     * Describes one backend component, like storage.
     * The health endpoint uses it to show status and a short reason in plain English.
     * That makes the response easier to read when something is wrong.
     */
    public static class ComponentStatus {
        @JsonProperty("status")
        private String status;

        @JsonProperty("detail")
        private String detail;

        public ComponentStatus() {
        }

        public ComponentStatus(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }

    /**
     * Describes the cache state used by the stats service.
     * It shows whether the cache is warm, how old it is, and how many hits or misses it has seen.
     * This keeps the health payload useful without exposing internal cache code.
     */
    public static class CacheStatus {
        @JsonProperty("status")
        private String status;

        @JsonProperty("warm")
        private boolean warm;

        @JsonProperty("ageMs")
        private long ageMs;

        @JsonProperty("ttlMs")
        private long ttlMs;

        @JsonProperty("hits")
        private long hits;

        @JsonProperty("misses")
        private long misses;

        public CacheStatus() {
        }

        public CacheStatus(String status, boolean warm, long ageMs, long ttlMs, long hits, long misses) {
            this.status = status;
            this.warm = warm;
            this.ageMs = ageMs;
            this.ttlMs = ttlMs;
            this.hits = hits;
            this.misses = misses;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public boolean isWarm() {
            return warm;
        }

        public void setWarm(boolean warm) {
            this.warm = warm;
        }

        public long getAgeMs() {
            return ageMs;
        }

        public void setAgeMs(long ageMs) {
            this.ageMs = ageMs;
        }

        public long getTtlMs() {
            return ttlMs;
        }

        public void setTtlMs(long ttlMs) {
            this.ttlMs = ttlMs;
        }

        public long getHits() {
            return hits;
        }

        public void setHits(long hits) {
            this.hits = hits;
        }

        public long getMisses() {
            return misses;
        }

        public void setMisses(long misses) {
            this.misses = misses;
        }
    }

    /**
     * Describes how much demo data is currently loaded.
     * It reports the number of users and tasks in memory.
     * That gives the health endpoint a quick view of whether the app has usable data.
     */
    public static class DataStatus {
        @JsonProperty("users")
        private int users;

        @JsonProperty("tasks")
        private int tasks;

        public DataStatus() {
        }

        public DataStatus(int users, int tasks) {
            this.users = users;
            this.tasks = tasks;
        }

        public int getUsers() {
            return users;
        }

        public void setUsers(int users) {
            this.users = users;
        }

        public int getTasks() {
            return tasks;
        }

        public void setTasks(int tasks) {
            this.tasks = tasks;
        }
    }
}
