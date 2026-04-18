package com.personal.happygallery.application.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = RetryPolicy.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
        backoff = @Backoff(
                delay = RetryPolicy.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                multiplier = RetryPolicy.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                random = true))
public @interface OptimisticLockRetryable {
}
