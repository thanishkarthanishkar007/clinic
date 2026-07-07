package com.devsclinic.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health check endpoint to verify the backend and its
 * dependencies (e.g. MongoDB connection) are up and reachable.
 * Useful for uptime monitors and deployment smoke tests.
 *
 * GET /api/health
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "Devs Hair and Skin Clinic - Appointment API"
        );
    }
}
