package com.personal.happygallery.application.config;

public final class RetryPolicy {

    public static final int OPTIMISTIC_LOCK_MAX_ATTEMPTS = 3;
    public static final long OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS = 50L;
    public static final double OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER = 2.0;

    private RetryPolicy() {}
}
