package com.developer.test.service;

import com.developer.test.dto.HealthResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Builds the health response for the backend.
 * It combines storage status, cache status, and data counts into one payload.
 * That gives the frontend and reviewers one place to see whether the app is healthy.
 */
@Service
public class HealthService {
    private final DataStore dataStore;
    private final StatsService statsService;

    public HealthService(DataStore dataStore, StatsService statsService) {
        this.dataStore = dataStore;
        this.statsService = statsService;
    }

    public HealthResponse getHealth() {
        StatsService.CacheSnapshot cacheSnapshot = statsService.getCacheSnapshot();
        boolean storageReady = dataStore.isPersistenceReady();
        String overallStatus = storageReady ? "ok" : "degraded";

        HealthResponse response = new HealthResponse();
        response.setStatus(overallStatus);
        response.setMessage("Java backend is running");
        response.setTimestamp(Instant.now());
        response.setVersion(resolveVersion());
        response.setStorage(new HealthResponse.ComponentStatus(
                storageReady ? "ok" : "degraded",
                dataStore.getStorageSourceDetail()
        ));
        response.setDataSource(new HealthResponse.ComponentStatus(
                dataStore.isMysqlMode() ? "ok" : "degraded",
                dataStore.getStorageSourceLabel()
        ));
        response.setCache(new HealthResponse.CacheStatus(
                cacheSnapshot.getStatus(),
                cacheSnapshot.isWarm(),
                cacheSnapshot.getAgeMillis(),
                cacheSnapshot.getTtlMillis(),
                cacheSnapshot.getHits(),
                cacheSnapshot.getMisses()
        ));
        response.setData(new HealthResponse.DataStatus(
                dataStore.getUserCount(),
                dataStore.getTaskCount()
        ));
        return response;
    }

    private String resolveVersion() {
        Package appPackage = HealthService.class.getPackage();
        String version = appPackage == null ? null : appPackage.getImplementationVersion();
        return version == null ? "dev" : version;
    }
}
