package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.batch.BatchExecutor;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.payment.port.in.PaymentAttemptExpiryBatchUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
        // payment_attempt.created_at은 UTC DB 시각으로 생성되므로 같은 시간대의 cutoff와 비교한다.
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                clock.instant().minus(PREPARE_TTL), ZoneOffset.UTC);
        List<Long> attemptIds = attemptReader.findExpiredPendingIds(cutoff, BATCH_SIZE);
        return BatchExecutor.execute(
                attemptIds,
                attemptId -> attemptId,
                attemptId -> expiryProcessor.expire(attemptId, cutoff),
                "결제 준비 만료");
    }
}
