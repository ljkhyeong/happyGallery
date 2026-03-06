package com.personal.happygallery.infra.payment;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 개발용 가짜 PG 어댑터.
 * 항상 성공 응답을 반환한다. 실패 시나리오는 테스트에서 Mock으로 대체한다.
 */
@Component("paymentProviderDelegate")
public class FakePaymentProvider implements PaymentProvider {

    @Override
    public RefundResult refund(String pgRef, long amount) {
        String fakeRef = "FAKE-REFUND-" + UUID.randomUUID();
        return RefundResult.success(fakeRef);
    }
}
