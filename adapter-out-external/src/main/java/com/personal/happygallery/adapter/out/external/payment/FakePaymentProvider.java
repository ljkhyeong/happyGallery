package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentLookupResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.application.payment.port.out.RefundLookupResult;
import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발·테스트용 가짜 PG 어댑터.
 * prod 프로필에서는 {@link TossPaymentsProvider}가 대신 빈으로 등록된다.
 */
@Component("paymentProviderDelegate")
@Profile("!prod")
public class FakePaymentProvider implements PaymentPort {

    private final LocalRefundFailureScript localRefundFailureScript;
    private final Clock clock;
    private final Map<String, PaymentLookupResult> confirmedPayments = new ConcurrentHashMap<>();
    private final Map<String, RefundLookupResult> refundedPaymentsByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, String> refundTransactionsByIdempotencyKey = new ConcurrentHashMap<>();

    public FakePaymentProvider(ObjectProvider<LocalRefundFailureScript> localRefundFailureScriptProvider,
                               Clock clock) {
        this.localRefundFailureScript = localRefundFailureScriptProvider.getIfAvailable();
        this.clock = clock;
    }

    @Override
    public PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey) {
        confirmedPayments.put(orderId, PaymentLookupResult.approved(paymentKey, orderId, amount));
        return PaymentConfirmResult.success(
                paymentKey,
                "FAKE_PG",
                OffsetDateTime.now(clock).toString());
    }

    @Override
    public PaymentLookupResult lookupByOrderId(String orderId) {
        return confirmedPayments.getOrDefault(
                orderId,
                PaymentLookupResult.notApproved(orderId, "가짜 PG에 승인된 결제가 없습니다."));
    }

    @Override
    public RefundResult refund(String paymentKey, long amount, String idempotencyKey) {
        if (localRefundFailureScript != null) {
            var reason = localRefundFailureScript.consumeNextFailureReason();
            if (reason.isPresent()) {
                return RefundResult.failure(reason.get());
            }
        }
        String refundTransactionKey = refundTransactionsByIdempotencyKey.computeIfAbsent(
                idempotencyKey, key -> "FAKE-REFUND-" + UUID.randomUUID());
        refundedPaymentsByIdempotencyKey.put(
                idempotencyKey,
                RefundLookupResult.refunded(paymentKey, amount, refundTransactionKey));
        return RefundResult.success(refundTransactionKey);
    }

    @Override
    public RefundLookupResult lookupRefund(String paymentKey, long amount, String idempotencyKey) {
        return refundedPaymentsByIdempotencyKey.getOrDefault(
                idempotencyKey,
                RefundLookupResult.notRefunded(paymentKey, "가짜 PG에 완료된 환불이 없습니다."));
    }
}
