package com.example.marketplace.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Database connectivity and application health checks")
public class HealthController {

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public HealthController(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @GetMapping
    @Operation(summary = "Application and database health check")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", Instant.now().toString());

        try (Connection conn = dataSource.getConnection()) {
            result.put("database", "CONNECTED");
            result.put("dbProduct", conn.getMetaData().getDatabaseProductName());
            result.put("dbVersion", conn.getMetaData().getDatabaseProductVersion());
        } catch (Exception e) {
            result.put("database", "UNREACHABLE");
            result.put("dbError", e.getMessage());
            result.put("status", "DOWN");
            return ResponseEntity.status(503).body(result);
        }

        Map<String, Object> counts = new LinkedHashMap<>();
        try {
            counts.put("users", jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        } catch (Exception ignored) {
            counts.put("users", "error");
        }
        try {
            counts.put("categories", jdbc.queryForObject("SELECT COUNT(*) FROM categories", Long.class));
        } catch (Exception ignored) {
            counts.put("categories", "error");
        }
        try {
            counts.put("services", jdbc.queryForObject("SELECT COUNT(*) FROM service_listings", Long.class));
        } catch (Exception ignored) {
            counts.put("services", "error");
        }
        try {
            counts.put("bookings", jdbc.queryForObject("SELECT COUNT(*) FROM bookings", Long.class));
        } catch (Exception ignored) {
            counts.put("bookings", "error");
        }
        try {
            counts.put("payments", jdbc.queryForObject("SELECT COUNT(*) FROM payments", Long.class));
        } catch (Exception ignored) {
            counts.put("payments", "error");
        }
        result.put("recordCounts", counts);

        return ResponseEntity.ok(result);
    }
}
