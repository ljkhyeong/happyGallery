package com.personal.happygallery.application.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;

/**
 * 낙관적 락 충돌을 첫 시도 포함 최대 3회까지 실행한다.
 *
 * <p>재시도 advisor는 기존 트랜잭션 advisor 앞에 붙으므로 재시도마다 새 트랜잭션에서 다시 읽는다.
 * 지연은 50ms에서 2배씩 늘고, 같은 비율로 커지는 jitter로 동시 재시도를 흩뜨린다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
        includes = ObjectOptimisticLockingFailureException.class,
        maxRetries = 2,
        delay = 50,
        jitter = 50,
        multiplier = 2.0)
public @interface OptimisticLockRetryable {
}
