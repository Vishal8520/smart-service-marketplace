package com.example.marketplace.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    @DisplayName("Allows up to 5 login attempts per minute, blocks 6th")
    void loginRateLimiting_blocksAfterFiveAttempts() {
        String key = "test-client-ip";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.tryConsumeLogin(key), "Attempt " + (i + 1) + " should be allowed");
        }

        assertFalse(rateLimitingService.tryConsumeLogin(key), "6th attempt should be blocked by rate limiter");
    }

    @Test
    @DisplayName("Different keys have independent login rate limit buckets")
    void loginRateLimiting_independentBucketsPerKey() {
        String key1 = "192.168.1.1";
        String key2 = "192.168.1.2";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitingService.tryConsumeLogin(key1));
        }
        assertFalse(rateLimitingService.tryConsumeLogin(key1));

        // key2 should still have its full quota
        assertTrue(rateLimitingService.tryConsumeLogin(key2));
    }

    @Test
    @DisplayName("General API bucket allows requests up to limit")
    void generalRateLimiting_allowsRequests() {
        String key = "api-client-1";
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitingService.tryConsumeGeneral(key));
        }
    }
}
