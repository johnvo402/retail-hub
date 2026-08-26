package com.johnvo.retailhub.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    @GetMapping
    HealthResponse health() {
        return new HealthResponse("UP", Instant.now());
    }

    record HealthResponse(String status, Instant timestamp) {
    }
}

