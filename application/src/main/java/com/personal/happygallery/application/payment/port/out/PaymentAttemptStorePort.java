package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentAttempt;

public interface PaymentAttemptStorePort {
    PaymentAttempt save(PaymentAttempt attempt);

    /** confirm 경로에서 상태/paymentKey 갱신을 즉시 반영해야 할 때 사용한다. */
    PaymentAttempt saveAndFlush(PaymentAttempt attempt);
}
