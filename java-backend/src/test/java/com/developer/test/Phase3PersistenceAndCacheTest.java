package com.developer.test;

import com.developer.test.model.Task;
import com.developer.test.model.User;
import com.developer.test.service.DataStore;
import com.developer.test.service.StatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class Phase3PersistenceAndCacheTest {
    private static final Path STORE_FILE = Paths.get("target", "test-phase3-store.json");

    @BeforeEach
    void resetStore() throws Exception {
        Files.deleteIfExists(STORE_FILE);
    }

    @Test
    void persistsNewRecordsAndReloadsThemFromDisk() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataStore firstStore = new DataStore(objectMapper, STORE_FILE.toString());

        User createdUser = firstStore.createUser("Priya Patel", "priya@example.com", "engineer");
        Task createdTask = firstStore.createTask("Document phase 3", "pending", createdUser.getId());

        DataStore reloadedStore = new DataStore(objectMapper, STORE_FILE.toString());

        assertThat(reloadedStore.getUserById(createdUser.getId())).isNotNull();
        assertThat(reloadedStore.getUserById(createdUser.getId()).getEmail()).isEqualTo("priya@example.com");
        assertThat(reloadedStore.getTaskById(createdTask.getId())).isNotNull();
        assertThat(reloadedStore.getTaskById(createdTask.getId()).getTitle()).isEqualTo("Document phase 3");
    }

    @Test
    void cachesStatsUntilTheCacheIsInvalidated() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DataStore store = new DataStore(objectMapper, STORE_FILE.toString());
        StatsService statsService = new StatsService(store, 60_000L);

        int initialUserTotal = statsService.getStats().getUsers().getTotal();

        // The cache should keep returning the previous snapshot until we explicitly invalidate it.
        store.createUser("Cache Test", "cache@example.com", "engineer");
        assertThat(statsService.getStats().getUsers().getTotal()).isEqualTo(initialUserTotal);

        statsService.invalidate();
        assertThat(statsService.getStats().getUsers().getTotal()).isEqualTo(initialUserTotal + 1);
    }
}
