package com.example.flood.security.application;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class ClientRateLimiter {

    private final ConcurrentHashMap<Long, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(long clientId, int limitPerMinute, Instant now) {
        if (limitPerMinute <= 0) {
            return false;
        }
        return buckets.computeIfAbsent(clientId, ignored -> new Bucket(limitPerMinute, now))
            .tryAcquire(limitPerMinute, now);
    }

    private static final class Bucket {
        private double tokens;
        private Instant lastRefill;

        private Bucket(int capacity, Instant now) {
            tokens = capacity;
            lastRefill = now;
        }

        private synchronized boolean tryAcquire(int capacity, Instant now) {
            double elapsedSeconds = Math.max(0, (now.toEpochMilli() - lastRefill.toEpochMilli()) / 1000.0);
            tokens = Math.min(capacity, tokens + elapsedSeconds * capacity / 60.0);
            lastRefill = now;
            if (tokens < 1.0) {
                return false;
            }
            tokens -= 1.0;
            return true;
        }
    }
}
