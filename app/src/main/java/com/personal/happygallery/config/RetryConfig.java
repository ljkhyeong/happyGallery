package com.personal.happygallery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 1)
public class RetryConfig {

    public static final int OPTIMISTIC_LOCK_MAX_ATTEMPTS = 3;
    public static final long OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS = 50L;
    public static final double OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER = 2.0;
}
