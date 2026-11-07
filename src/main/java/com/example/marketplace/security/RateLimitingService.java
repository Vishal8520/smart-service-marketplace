package com.example.marketplace.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalApiBuckets = new ConcurrentHashMap<>();

    public boolean tryConsumeLogin(String key) {
        Bucket bucket = loginBuckets.computeIfAbsent(key, k -> createLoginBucket());
        return bucket.tryConsume(1);
    }

    public boolean tryConsumeGeneral(String key) {
        Bucket bucket = generalApiBuckets.computeIfAbsent(key, k -> createGeneralBucket());
        return bucket.tryConsume(1);
    }

    private Bucket createLoginBucket() {
        // Allow 5 login attempts per minute with graceful refill
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createGeneralBucket() {
        // Allow 60 requests per minute
        Bandwidth limit = Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
