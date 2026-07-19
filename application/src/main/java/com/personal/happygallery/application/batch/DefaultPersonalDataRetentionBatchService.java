package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.customer.PhoneVerificationRetentionService;
import com.personal.happygallery.application.payment.PaymentAttemptSensitiveDataCleanupProcessor;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;

@Service
public class DefaultPersonalDataRetentionBatchService implements PersonalDataRetentionBatchUseCase {

    public static final Duration PAYMENT_ATTEMPT_RETENTION = Duration.ofDays(30);
    public static final Duration PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY = Duration.ofDays(1);
    private static final int PAGE_SIZE = 100;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor;
    private final PhoneVerificationRetentionService verificationRetentionService;
    private final Clock clock;

    public DefaultPersonalDataRetentionBatchService(
            PaymentAttemptReaderPort attemptReader,
            PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor,
            PhoneVerificationRetentionService verificationRetentionService,
            Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptCleanupProcessor = attemptCleanupProcessor;
        this.verificationRetentionService = verificationRetentionService;
        this.clock = clock;
    }

    @Override
    public BatchResult cleanUpExpiredSensitiveData() {
        LocalDateTime paymentCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(PAYMENT_ATTEMPT_RETENTION), ZoneOffset.UTC);
        BatchResult paymentResult = BatchExecutor.executeByIdCursor(
                afterId -> attemptReader.findSensitiveDataCleanupCandidateIds(
                        paymentCutoff, afterId, PAGE_SIZE),
                attemptId -> attemptId,
                attemptId -> attemptCleanupProcessor.clear(attemptId, paymentCutoff),
                "결제 개인정보 정리");

        LocalDateTime verificationCutoff = LocalDateTime.now(clock)
                .minus(PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY);
        int deletedVerificationCount = verificationRetentionService.deleteExpiredBefore(verificationCutoff);
        return paymentResult.merge(BatchResult.successOnly(deletedVerificationCount));
    }
}
