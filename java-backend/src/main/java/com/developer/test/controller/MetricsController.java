package com.developer.test.controller;

import com.developer.test.dto.ApiMetricsResponse;
import com.developer.test.service.MetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes runtime metrics for the backend in a simple JSON format.
 * It reports request volume, security activity, and basic performance data.
 * The controller stays thin and leaves all counting and shaping work to the service.
 */
@Tag(name = "Metrics", description = "Runtime telemetry for requests, security, and performance")
@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
public class MetricsController {
    private final MetricsService metricsService;
    private final org.springframework.core.env.Environment environment;

    public MetricsController(MetricsService metricsService, org.springframework.core.env.Environment environment) {
        this.metricsService = metricsService;
        this.environment = environment;
    }

    @GetMapping
    @Operation(summary = "Get metrics", description = "Returns the live runtime metrics snapshot for the backend.")
    public ResponseEntity<ApiMetricsResponse> getMetrics() {
        boolean apiKeyRequired = environment.getProperty("app.security.api-key") != null
                && !environment.getProperty("app.security.api-key").trim().isEmpty();
        return ResponseEntity.ok(metricsService.snapshot(apiKeyRequired));
    }
}
