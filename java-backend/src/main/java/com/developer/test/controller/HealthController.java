package com.developer.test.controller;

import com.developer.test.dto.HealthResponse;
import com.developer.test.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles the health check endpoint for the backend.
 * It gathers status data from the service layer and returns one JSON response.
 * Keeping the controller thin makes the endpoint easy to review and easy to test.
 */
@Tag(name = "Health", description = "Health and readiness checks for the backend")
@RestController
@RequestMapping
@CrossOrigin(origins = "*")
public class HealthController {
    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }
    
    @GetMapping("/health")
    @Operation(summary = "Get backend health", description = "Returns storage, cache, and data status for the service.")
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.getHealth();
        return ResponseEntity.ok(response);
    }
}
