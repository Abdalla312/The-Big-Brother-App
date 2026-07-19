package com.expensetracker.big_brother.config;

import lombok.Synchronized;

import java.time.Duration;
import java.time.Instant;

public class TokenBucket {
    private final int capacity;
    private final int refillRate;
    private final Duration refillPeriod;
    private double tokens;
    private Instant lastRefill;

    TokenBucket(RateLimitProperties props) {
        this.capacity = props.capacity();
        this.refillRate = props.refillRate();
        this.refillPeriod = Duration.ofSeconds(props.refillPeriodSeconds());
        this.tokens = capacity;
        this.lastRefill = Instant.now();
    }

    synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        long elapsedSecs = Duration.between(lastRefill, now).toSeconds();
        if (elapsedSecs == 0) return;
        double newTokens = (double) elapsedSecs / refillPeriod.getSeconds() * refillRate;
        tokens = Math.min(capacity, tokens + newTokens);
        lastRefill = now;
    }
    Instant lastRefill() { return lastRefill; }
}
