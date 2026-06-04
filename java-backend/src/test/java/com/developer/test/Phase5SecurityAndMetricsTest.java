package com.developer.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage for API key auth, write rate limiting, and runtime metrics.
 */
@SpringBootTest(properties = {
        "app.datastore.file=target/test-phase5-store.json",
        "app.security.rate-limit.write.requests-per-window=1",
        "app.security.rate-limit.window-ms=60000"
})
@AutoConfigureMockMvc
class Phase5SecurityAndMetricsTest {
    private static final Path STORE_FILE = Paths.get("target", "test-phase5-store.json");

    static {
        try {
            Files.deleteIfExists(STORE_FILE);
        } catch (Exception ignored) {
            // The test can still run if the file was already absent or in use by another process.
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken() throws Exception {
        String loginPayload = "{\"email\":\"ishant.sharma1947@gmail.com\",\"password\":\"admin123\"}";
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).get("token").asText();
    }

    @BeforeEach
    void resetStore() throws Exception {
        Files.deleteIfExists(STORE_FILE);
    }

    @Test
    void apiKeyAuthRateLimitingAndMetricsAreExposedTogether() throws Exception {
        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests.total").isNumber());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Blocked\",\"email\":\"blocked@example.com\",\"role\":\"engineer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("A valid Bearer token is required"));

        String token = adminToken();
        String payload = "{\"name\":\"Maya Chen\",\"email\":\"maya@example.com\",\"role\":\"engineer\"}";

        String firstCreate = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdUser = objectMapper.readTree(firstCreate);
        assertThat(createdUser.get("email").asText()).isEqualTo("maya@example.com");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rate Limited\",\"email\":\"limit@example.com\",\"role\":\"engineer\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Write rate limit exceeded. Try again later."));

        mockMvc.perform(get("/api/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.security.apiKeyRequired").value(true))
                .andExpect(jsonPath("$.security.authFailures").value(1))
                .andExpect(jsonPath("$.security.rateLimited").value(1))
                .andExpect(jsonPath("$.requests.total").isNumber())
                .andExpect(jsonPath("$.performance.averageDurationMs").isNumber())
                .andExpect(jsonPath("$.endpoints").exists());
    }
}
