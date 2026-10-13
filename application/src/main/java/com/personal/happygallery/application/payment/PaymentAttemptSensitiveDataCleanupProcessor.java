package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAttemptSensitiveDataCleanupProcessor {

    private final PaymentAttemptReaderPort attemptReader;

    public PaymentAttemptSensitiveDataCleanupProcessor(PaymentAttemptReaderPort attemptReader) {
        this.attemptReader = attemptReader;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean clear(Long attemptId, LocalDateTime cutoff) {
        PaymentAttempt attempt = attemptReader.findByIdForUpdate(attemptId).orElse(null);
        return attempt != null && attempt.clearSensitiveDataBefore(cutoff);
    }
}
