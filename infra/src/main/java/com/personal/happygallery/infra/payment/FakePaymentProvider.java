package com.personal.happygallery.infra.payment;

import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * 개발용 가짜 PG 어댑터.
 * 기본적으로 성공 응답을 반환하되, local 프로필에서는 one-shot 실패 훅을 지원한다.
 */
@Component("paymentProviderDelegate")
public class FakePaymentProvider implements PaymentProvider {

    private final LocalRefundFailureScript localRefundFailureScript;

    public FakePaymentProvider(@Nullable LocalRefundFailureScript localRefundFailureScript) {
        this.localRefundFailureScript = localRefundFailureScript;
    }

    @Override
    public RefundResult refund(String pgRef, long amount) {
        if (localRefundFailureScript != null) {
            var failureReason = localRefundFailureScript.consumeIfMatches(RefundContext.currentOrderId());
            if (failureReason.isPresent()) {
                return RefundResult.failure(failureReason.get());
            }
        }
        String fakeRef = "FAKE-REFUND-" + UUID.randomUUID();
        return RefundResult.success(fakeRef);
    }
}
