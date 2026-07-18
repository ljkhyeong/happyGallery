package com.personal.happygallery.adapter.in.web.ratelimit;

import java.time.Duration;

public record RateLimitDecision(
        long limit,
        long remaining,
        Duration window,
        boolean rejected
) {
}
