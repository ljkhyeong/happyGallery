package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.util.Optional;

public interface PaymentAttemptReaderPort {
    Optional<PaymentAttempt> findById(Long id);

    /** Toss 응답의 orderId(=order_id_external, UUID 문자열)로 prepare 레코드를 조회한다. */
    Optional<PaymentAttempt> findByOrderIdExternal(String orderIdExternal);
}
