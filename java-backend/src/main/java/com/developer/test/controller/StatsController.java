package com.developer.test.controller;

import com.developer.test.dto.StatsResponse;
import com.developer.test.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the stats endpoint used by the frontend and interview checks.
 * The controller only asks the service for the current numbers and returns them as JSON.
 * This keeps request handling simple and moves the logic into one shared place.
 */
@Tag(name = "Stats", description = "Summary counts for users and tasks")
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {
    
    private final StatsService statsService;
    
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }
    
    @GetMapping
    @Operation(summary = "Get stats", description = "Returns totals for users and tasks, including task status breakdown.")
    public ResponseEntity<StatsResponse> getStats() {
        StatsResponse stats = statsService.getStats();
        return ResponseEntity.ok(stats);
    }
}
