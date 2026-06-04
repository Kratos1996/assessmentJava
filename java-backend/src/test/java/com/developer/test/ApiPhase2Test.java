package com.developer.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiPhase2Test {
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

    @Test
    void createsUserAndReturnsTheStoredRecord() throws Exception {
        String token = adminToken();
        String payload = "{"
                + "\"name\":\"Ava Stone\","
                + "\"email\":\"ava.stone@example.com\","
                + "\"role\":\"engineer\""
                + "}";

        String responseBody = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Ava Stone"))
                .andExpect(jsonPath("$.email").value("ava.stone@example.com"))
                .andExpect(jsonPath("$.role").value("engineer"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdUser = objectMapper.readTree(responseBody);
        int createdId = createdUser.get("id").asInt();

        mockMvc.perform(get("/api/users/" + createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdId))
                .andExpect(jsonPath("$.email").value("ava.stone@example.com"));
    }

    @Test
    void createsTaskAndValidatesUserId() throws Exception {
        String token = adminToken();
        String payload = "{"
                + "\"title\":\"Review pull request\","
                + "\"status\":\"pending\","
                + "\"userId\":1"
                + "}";

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Review pull request"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void rejectsInvalidTaskStatusWithStructuredError() throws Exception {
        String token = adminToken();
        String payload = "{"
                + "\"title\":\"Ship release\","
                + "\"status\":\"queued\","
                + "\"userId\":1"
                + "}";

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("status must be pending, in-progress, or completed"));
    }

    @Test
    void updatesTaskPartiallyAndKeepsOtherFieldsIntact() throws Exception {
        String token = adminToken();
        mockMvc.perform(put("/api/tasks/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Implement authentication and review flow\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Implement authentication and review flow"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.userId").value(1));
    }
}
