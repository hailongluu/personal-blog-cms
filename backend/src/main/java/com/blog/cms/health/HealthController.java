package com.blog.cms.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

/**
 * Public health check — used by Docker HEALTHCHECK + load balancer + uptime monitoring.
 * No auth required.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    @Autowired
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString(),
                "service", "blog-cms-backend",
                "version", "0.1.0"
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Detailed health — checks DB connectivity.
     * Useful for debugging connection issues.
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> dbHealth() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            Map<String, Object> response = Map.of(
                    "status", valid ? "ok" : "error",
                    "database", "connected",
                    "valid", valid,
                    "timestamp", Instant.now().toString()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                    "status", "error",
                    "database", "disconnected",
                    "error", e.getMessage(),
                    "timestamp", Instant.now().toString()
            );
            return ResponseEntity.status(503).body(response);
        }
    }
}
