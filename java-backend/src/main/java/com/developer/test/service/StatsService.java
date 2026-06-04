package com.developer.test.service;

import com.developer.test.dto.StatsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Caches stats in memory so repeated reads stay fast.
 * It also tracks cache hits and misses so we can explain what the app is doing.
 * When user or task data changes, the cache is cleared and rebuilt on demand.
 */
@Service
public class StatsService {
    private final DataStore dataStore;
    private final long cacheTtlMillis;
    private final AtomicReference<CachedStats> cache = new AtomicReference<>();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong cacheMisses = new AtomicLong();

    public StatsService(DataStore dataStore,
                        @Value("${app.stats.cache-ttl-ms:5000}") long cacheTtlMillis) {
        this.dataStore = dataStore;
        this.cacheTtlMillis = cacheTtlMillis;
    }

    public StatsResponse getStats() {
        CachedStats current = cache.get();
        long now = System.currentTimeMillis();

        if (current != null && current.isFresh(now, cacheTtlMillis)) {
            cacheHits.incrementAndGet();
            return current.getStats();
        }

        cacheMisses.incrementAndGet();
        StatsResponse stats = dataStore.getStats();
        cache.set(new CachedStats(stats, now));
        return stats;
    }

    public void invalidate() {
        cache.set(null);
    }

    public CacheSnapshot getCacheSnapshot() {
        CachedStats current = cache.get();
        long now = System.currentTimeMillis();
        boolean warm = current != null && current.isFresh(now, cacheTtlMillis);

        return new CacheSnapshot(
                warm ? "ok" : "cold",
                warm,
                current == null ? -1 : now - current.getCreatedAtMillis(),
                cacheTtlMillis,
                cacheHits.get(),
                cacheMisses.get()
        );
    }

    private static class CachedStats {
        private final StatsResponse stats;
        private final long createdAtMillis;

        private CachedStats(StatsResponse stats, long createdAtMillis) {
            this.stats = stats;
            this.createdAtMillis = createdAtMillis;
        }

        private StatsResponse getStats() {
            return stats;
        }

        private long getCreatedAtMillis() {
            return createdAtMillis;
        }

        private boolean isFresh(long now, long ttlMillis) {
            return now - createdAtMillis < ttlMillis;
        }
    }

    /**
     * Captures the cache state for health checks and metrics.
     * It reports whether the cache is warm, how old it is, and how many hits or misses it has seen.
     * This keeps cache behavior easy to explain without exposing the internal cache object.
     */
    public static class CacheSnapshot {
        private final String status;
        private final boolean warm;
        private final long ageMillis;
        private final long ttlMillis;
        private final long hits;
        private final long misses;

        public CacheSnapshot(String status, boolean warm, long ageMillis, long ttlMillis, long hits, long misses) {
            this.status = status;
            this.warm = warm;
            this.ageMillis = ageMillis;
            this.ttlMillis = ttlMillis;
            this.hits = hits;
            this.misses = misses;
        }

        public String getStatus() {
            return status;
        }

        public boolean isWarm() {
            return warm;
        }

        public long getAgeMillis() {
            return ageMillis;
        }

        public long getTtlMillis() {
            return ttlMillis;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public boolean isHealthy() {
            return warm || ageMillis >= -1;
        }
    }
}
