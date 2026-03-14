package com.personal.happygallery.infra.payment;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * local 프로필에서 다음 환불 1회를 강제로 실패시키는 one-shot 훅.
 * targetOrderId를 지정하면 해당 주문의 환불만 실패시킨다.
 */
@Profile("local")
@Component
public class LocalRefundFailureScript {

    private final AtomicReference<String> nextFailureReason = new AtomicReference<>();
    private final AtomicReference<Long> targetOrderId = new AtomicReference<>();

    public void armNextFailure(String reason) {
        armNextFailure(reason, null);
    }

    public void armNextFailure(String reason, Long orderId) {
        nextFailureReason.set(reason);
        targetOrderId.set(orderId);
    }

    /**
     * 현재 환불 대상이 armed 조건과 일치하면 failure reason을 소비하고 반환한다.
     * targetOrderId가 설정되어 있고 현재 orderId와 다르면 소비하지 않는다.
     */
    public Optional<String> consumeIfMatches(Long currentOrderId) {
        String reason = nextFailureReason.get();
        if (reason == null) {
            return Optional.empty();
        }

        Long target = targetOrderId.get();
        if (target != null && !target.equals(currentOrderId)) {
            return Optional.empty();
        }

        // 조건 일치: 원자적으로 소비
        if (nextFailureReason.compareAndSet(reason, null)) {
            targetOrderId.set(null);
            return Optional.of(reason);
        }
        return Optional.empty();
    }

    public void clear() {
        nextFailureReason.set(null);
        targetOrderId.set(null);
    }
}
