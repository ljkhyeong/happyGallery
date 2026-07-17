package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 가짜 PG 어댑터.
 * prod 프로필에서는 {@link TossPaymentsProvider}가 대신 빈으로 등록된다.
 */
@Component("paymentProviderDelegate")
@Profile("!prod")
public class FakePaymentProvider implements PaymentProvider {

    private final LocalRefundFailureScript localRefundFailureScript;

    public FakePaymentProvider(ObjectProvider<LocalRefundFailureScript> localRefundFailureScriptProvider) {
        this.localRefundFailureScript = localRefundFailureScriptProvider.getIfAvailable();
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        return PaymentConfirmResult.success(
                "FAKE-PG-" + UUID.randomUUID(),
                "FAKE_PG",
                OffsetDateTime.now().toString());
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        if (localRefundFailureScript != null) {
            var reason = localRefundFailureScript.consumeNextFailureReason();
            if (reason.isPresent()) {
                return RefundResult.failure(reason.get());
            }
        }
        return RefundResult.success("FAKE-REFUND-" + UUID.randomUUID());
    }
}
