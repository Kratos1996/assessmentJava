package com.developer.test;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.datastore.file=target/test-phase3-health.json",
        "app.stats.cache-ttl-ms=60000"
})
@AutoConfigureMockMvc
class Phase3HealthAndFilterTest {
    private static final Path STORE_FILE = Paths.get("target", "test-phase3-health.json");

    static {
        try {
            Files.deleteIfExists(STORE_FILE);
        } catch (Exception ignored) {
            // The test still runs if the file did not exist or was already locked by a previous cleanup.
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsStorageAndCacheDetails() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.storage.status").exists())
                .andExpect(jsonPath("$.storage.detail").exists())
                .andExpect(jsonPath("$.cache.status").exists())
                .andExpect(jsonPath("$.data.users").isNumber())
                .andExpect(jsonPath("$.data.tasks").isNumber());
    }

    @Test
    void writeRequestsWithoutJsonContentTypeAreRejectedEarly() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("X-API-Key", "dev-api-key")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Bad Request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json for write operations"));
    }
}
