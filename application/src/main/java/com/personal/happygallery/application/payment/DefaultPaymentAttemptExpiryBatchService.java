package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.PaymentAttemptExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class DefaultPaymentAttemptExpiryBatchService implements PaymentAttemptExpiryBatchUseCase {

    static final Duration PREPARE_TTL = Duration.ofMinutes(30);
    private static final int BATCH_SIZE = 100;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptExpiryProcessor expiryProcessor;
    private final Clock clock;

    public DefaultPaymentAttemptExpiryBatchService(PaymentAttemptReaderPort attemptReader,
                                                    PaymentAttemptExpiryProcessor expiryProcessor,
                                                    Clock clock) {
        this.attemptReader = attemptReader;
        this.expiryProcessor = expiryProcessor;
        this.clock = clock;
    }

    @Override
    public BatchResult expirePendingAttempts() {
        LocalDateTime cutoff = prepareExpiredBefore(clock.instant());
        return BatchExecutor.executeByIdCursor(
                afterId -> attemptReader.findExpiredPendingIdsAfterId(
                        cutoff, afterId, BATCH_SIZE),
                attemptId -> attemptId,
                attemptId -> expiryProcessor.expire(attemptId, cutoff),
                "결제 준비 만료");
    }

    /** payment_attempt.created_at은 UTC DB 시각으로 생성되므로 같은 시간대의 만료 경계를 돌려준다. */
    static LocalDateTime prepareExpiredBefore(Instant now) {
        return LocalDateTime.ofInstant(now.minus(PREPARE_TTL), ZoneOffset.UTC);
    }
}
