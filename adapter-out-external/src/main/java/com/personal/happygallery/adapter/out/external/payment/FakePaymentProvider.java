package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 가짜 PG 어댑터.
 * prod 프로필에서는 {@link TossPaymentsProvider}가 대신 빈으로 등록된다.
 */
@Component("paymentProviderDelegate")
@Profile("!prod")
public class FakePaymentProvider implements PaymentProvider {

    private LocalRefundFailureScript localRefundFailureScript;

    public FakePaymentProvider() {
    }

    FakePaymentProvider(LocalRefundFailureScript localRefundFailureScript) {
        this.localRefundFailureScript = localRefundFailureScript;
    }

    @Autowired(required = false)
    void setLocalRefundFailureScript(LocalRefundFailureScript localRefundFailureScript) {
        this.localRefundFailureScript = localRefundFailureScript;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount) {
        return PaymentConfirmResult.success(
                "FAKE-PG-" + UUID.randomUUID(),
                "FAKE_PG",
                OffsetDateTime.now().toString());
    }

    @Override
    public RefundResult refund(String pgRef, long amount) {
        if (localRefundFailureScript != null) {
            var reason = localRefundFailureScript.consumeNextFailureReason();
            if (reason.isPresent()) {
                return RefundResult.failure(reason.get());
            }
        }
        return RefundResult.success("FAKE-REFUND-" + UUID.randomUUID());
    }
}
