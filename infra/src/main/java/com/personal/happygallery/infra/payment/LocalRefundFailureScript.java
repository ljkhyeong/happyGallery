package com.personal.happygallery.infra.payment;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local 프로필에서 다음 환불 1회를 강제로 실패시키는 one-shot 훅.
 */
@Profile("local")
@Component
public class LocalRefundFailureScript {

    private final AtomicReference<String> nextFailureReason = new AtomicReference<>();

    public void armNextFailure(String reason) {
        nextFailureReason.set(reason);
    }

    public Optional<String> consumeNextFailureReason() {
        return Optional.ofNullable(nextFailureReason.getAndSet(null));
    }

    public void clear() {
        nextFailureReason.set(null);
    }
}
