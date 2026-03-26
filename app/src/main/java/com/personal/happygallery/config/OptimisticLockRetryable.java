package com.personal.happygallery.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * 낙관적 락 충돌 시 재시도하는 메타 어노테이션.
 *
 * <p>{@link RetryConfig}의 상수를 단일 지점에서 참조하여
 * 서비스 메서드마다 {@code @Retryable} 보일러플레이트를 반복하지 않도록 한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = RetryConfig.OPTIMISTIC_LOCK_MAX_ATTEMPTS,
        backoff = @Backoff(
                delay = RetryConfig.OPTIMISTIC_LOCK_INITIAL_DELAY_MILLIS,
                multiplier = RetryConfig.OPTIMISTIC_LOCK_BACKOFF_MULTIPLIER,
                random = true))
public @interface OptimisticLockRetryable {
}
