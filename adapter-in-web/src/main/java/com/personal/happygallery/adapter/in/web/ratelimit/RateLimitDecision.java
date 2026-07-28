package com.personal.happygallery.adapter.in.web.ratelimit;

import java.time.Duration;

public record RateLimitDecision(
        long limit,
        long remaining,
        Duration window,
        boolean rejected
) {

    public long retryAfterSeconds() {
        return Math.max(1, Math.ceilDiv(window.toMillis(), 1_000));
    }
}
