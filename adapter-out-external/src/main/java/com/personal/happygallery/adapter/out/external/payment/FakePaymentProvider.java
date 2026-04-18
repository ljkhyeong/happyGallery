package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.RefundResult;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 개발용 가짜 PG 어댑터. 항상 성공 응답을 반환한다.
 */
@Component("paymentProviderDelegate")
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
