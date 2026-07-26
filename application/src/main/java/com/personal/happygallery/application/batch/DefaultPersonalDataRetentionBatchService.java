package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.admin.AdminAuthHistoryRetentionService;
import com.personal.happygallery.application.cart.CartMergeRequestRetentionService;
import com.personal.happygallery.application.customer.PhoneVerificationRetentionService;
import com.personal.happygallery.application.media.ImageMediaRetentionService;
import com.personal.happygallery.application.notification.NotificationRetentionService;
import com.personal.happygallery.application.payment.PaymentAttemptSensitiveDataCleanupProcessor;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.token.GuestTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultPersonalDataRetentionBatchService implements PersonalDataRetentionBatchUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DefaultPersonalDataRetentionBatchService.class);
    public static final Duration PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY = Duration.ofDays(1);
    public static final Duration CART_MERGE_REQUEST_RETENTION = Duration.ofDays(7);
    public static final Duration NOTIFICATION_RETENTION = Duration.ofDays(180);
    public static final Duration ADMIN_AUTH_HISTORY_RETENTION = Duration.ofDays(180);
    private static final int PAGE_SIZE = 100;
    private static final String PAYMENT_ATTEMPT = "payment_attempt";
    private static final String PHONE_VERIFICATION = "phone_verification";
    private static final String CART_MERGE_REQUEST = "cart_merge_request";
    private static final String IMAGE_MEDIA = "image_media";
    private static final String NOTIFICATION_LOG = "notification_log";
    private static final String NOTIFICATION_OUTBOX = "notification_outbox";
    private static final String ADMIN_AUTH_HISTORY = "admin_auth_history";

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor;
    private final PhoneVerificationRetentionService verificationRetentionService;
    private final CartMergeRequestRetentionService cartMergeRequestRetentionService;
    private final ImageMediaRetentionService imageMediaRetentionService;
    private final NotificationRetentionService notificationRetentionService;
    private final AdminAuthHistoryRetentionService adminAuthHistoryRetentionService;
    private final GuestTokenProperties guestTokenProperties;
    private final Clock clock;

    public DefaultPersonalDataRetentionBatchService(
            PaymentAttemptReaderPort attemptReader,
            PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor,
            PhoneVerificationRetentionService verificationRetentionService,
            CartMergeRequestRetentionService cartMergeRequestRetentionService,
            ImageMediaRetentionService imageMediaRetentionService,
            NotificationRetentionService notificationRetentionService,
            AdminAuthHistoryRetentionService adminAuthHistoryRetentionService,
            GuestTokenProperties guestTokenProperties,
            Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptCleanupProcessor = attemptCleanupProcessor;
        this.verificationRetentionService = verificationRetentionService;
        this.cartMergeRequestRetentionService = cartMergeRequestRetentionService;
        this.imageMediaRetentionService = imageMediaRetentionService;
        this.notificationRetentionService = notificationRetentionService;
        this.adminAuthHistoryRetentionService = adminAuthHistoryRetentionService;
        this.guestTokenProperties = guestTokenProperties;
        this.clock = clock;
    }

    @Override
    public BatchResult cleanUpExpiredSensitiveData() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime paymentCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()), ZoneOffset.UTC);
        LocalDateTime verificationCutoff = now.minus(PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY);
        LocalDateTime cartMergeCutoff = now.minus(CART_MERGE_REQUEST_RETENTION);
        LocalDateTime notificationCutoff = now.minus(NOTIFICATION_RETENTION);
        LocalDateTime adminAuthHistoryCutoff = now.minus(ADMIN_AUTH_HISTORY_RETENTION);

        BatchResult result = cleanUpPaymentAttempts(paymentCutoff);
        result = result.merge(deleteInBatches(
                PHONE_VERIFICATION,
                () -> verificationRetentionService.deleteBatchBefore(
                        verificationCutoff, PAGE_SIZE)));
        result = result.merge(deleteInBatches(
                CART_MERGE_REQUEST,
                () -> cartMergeRequestRetentionService.deleteBatchBefore(
                        cartMergeCutoff, PAGE_SIZE)));
        result = result.merge(isolateCountSource(
                IMAGE_MEDIA,
                imageMediaRetentionService::deleteUnreferencedImages));
        result = result.merge(deleteInBatches(
                NOTIFICATION_LOG,
                () -> notificationRetentionService.deleteChannelLogsBefore(
                        notificationCutoff, PAGE_SIZE)));
        result = result.merge(deleteInBatches(
                NOTIFICATION_OUTBOX,
                () -> notificationRetentionService.deleteTerminalOutboxesBefore(
                        notificationCutoff, PAGE_SIZE)));
        return result.merge(deleteInBatches(
                ADMIN_AUTH_HISTORY,
                () -> adminAuthHistoryRetentionService.deleteBatchBefore(
                        adminAuthHistoryCutoff, PAGE_SIZE)));
    }

    private BatchResult deleteInBatches(String source, IntSupplier deleteBatch) {
        int total = 0;
        try {
            int deleted;
            do {
                deleted = deleteBatch.getAsInt();
                total = Math.addExact(total, deleted);
            } while (deleted == PAGE_SIZE);
            return BatchResult.successOnly(total);
        } catch (Exception e) {
            logSourceFailure(source, e);
            return new BatchResult(total, 1, Map.of(source, 1));
        }
    }

    private BatchResult isolateCountSource(String source, IntSupplier operation) {
        try {
            return BatchResult.successOnly(operation.getAsInt());
        } catch (Exception e) {
            logSourceFailure(source, e);
            return BatchResult.of(0, Map.of(source, 1));
        }
    }

    private BatchResult cleanUpPaymentAttempts(LocalDateTime cutoff) {
        BatchResult total = BatchResult.successOnly(0);
        long afterId = 0L;
        try {
            while (true) {
                List<Long> attemptIds = attemptReader.findSensitiveDataCleanupCandidateIds(
                        cutoff, afterId, PAGE_SIZE);
                if (attemptIds.isEmpty()) {
                    return total;
                }

                BatchResult pageResult = BatchExecutor.execute(
                        attemptIds,
                        attemptId -> attemptId,
                        attemptId -> attemptCleanupProcessor.clear(attemptId, cutoff),
                        "결제 개인정보 정리");
                total = total.merge(new BatchResult(
                        pageResult.successCount(),
                        pageResult.failureCount(),
                        pageResult.failureCount() == 0
                                ? Map.of()
                                : Map.of(PAYMENT_ATTEMPT, pageResult.failureCount())));
                afterId = attemptIds.getLast();
            }
        } catch (Exception e) {
            logSourceFailure(PAYMENT_ATTEMPT, e);
            return total.merge(BatchResult.of(0, Map.of(PAYMENT_ATTEMPT, 1)));
        }
    }

    private void logSourceFailure(String source, Exception e) {
        log.warn("개인정보 보존 정리 실패 [source={} type={}]",
                source, e.getClass().getSimpleName(), e);
    }
}
