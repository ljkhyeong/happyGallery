package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptReaderPort {
    Optional<PaymentAttempt> findById(Long id);

    Optional<PaymentAttempt> findByIdForUpdate(Long id);

    /** Toss 응답의 orderId(=order_id_external, UUID 문자열)로 prepare 레코드를 조회한다. */
    Optional<PaymentAttempt> findByOrderIdExternal(String orderIdExternal);

    Optional<PaymentAttempt> findByOrderIdExternalForUpdate(String orderIdExternal);

    /** confirm 도중 중단된 PROCESSING/RETRYABLE/APPROVED 시도 ID를 오래된 순서로 조회한다. */
    List<Long> findConfirmRecoveryCandidateIds(LocalDateTime staleBefore, int limit);
}
