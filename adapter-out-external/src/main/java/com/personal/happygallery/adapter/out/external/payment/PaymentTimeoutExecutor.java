package com.personal.happygallery.adapter.out.external.payment;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** PG TimeLimiter 작업을 실행하고 애플리케이션 종료 시 짧게 정리한다. */
final class PaymentTimeoutExecutor implements Executor, AutoCloseable {

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(2);

    private final ExecutorService delegate;

    PaymentTimeoutExecutor(ExecutorService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @Override
    public void close() {
        delegate.shutdown();
        try {
            if (!delegate.awaitTermination(SHUTDOWN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                delegate.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            delegate.shutdownNow();
        }
    }
}
