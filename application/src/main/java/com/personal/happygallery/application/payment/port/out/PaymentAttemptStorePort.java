package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentAttempt;

public interface PaymentAttemptStorePort {
    PaymentAttempt save(PaymentAttempt attempt);
}
