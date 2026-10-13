package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.admin.port.out.AdminAuthHistoryPort;
import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertRetentionPort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort;
import com.personal.happygallery.application.customer.port.out.EmailVerificationStorePort;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationStorePort;
import com.personal.happygallery.application.media.ImageMediaRetentionService;
import com.personal.happygallery.application.notification.port.out.NotificationRetentionPort;
import com.personal.happygallery.application.payment.PaymentAttemptSensitiveDataCleanupProcessor;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.review.ReviewEvidenceRetentionService;
import com.personal.happygallery.application.review.port.out.ReviewTombstoneRetentionPort;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DefaultPersonalDataRetentionBatchService implements PersonalDataRetentionBatchUseCase {

    private static final Logger log =
            LoggerFactory.getLogger(DefaultPersonalDataRetentionBatchService.class);
    public static final Duration PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY = Duration.ofDays(1);
    public static final Duration EMAIL_VERIFICATION_RETENTION_AFTER_EXPIRY = Duration.ofDays(1);
    public static final Duration CART_MERGE_REQUEST_RETENTION = Duration.ofDays(7);
    public static final Duration NOTIFICATION_RETENTION = Duration.ofDays(180);
    public static final Duration ADMIN_AUTH_HISTORY_RETENTION = Duration.ofDays(180);
    public static final Duration VACANCY_ALERT_TERMINAL_RETENTION = Duration.ofDays(30);
    public static final Duration REVIEW_TOMBSTONE_RETENTION = Duration.ofDays(30);
    private static final int PAGE_SIZE = 100;
    private static final String PAYMENT_ATTEMPT = "payment_attempt";
    private static final String PHONE_VERIFICATION = "phone_verification";
    private static final String EMAIL_VERIFICATION = "email_verification";
    private static final String CART_MERGE_REQUEST = "cart_merge_request";
    private static final String IMAGE_MEDIA = "image_media";
    private static final String NOTIFICATION_LOG = "notification_log";
    private static final String NOTIFICATION_OUTBOX = "notification_outbox";
    private static final String ADMIN_AUTH_HISTORY = "admin_auth_history";
    private static final String REVIEW_EVIDENCE = "review_evidence";
    private static final String REVIEW_TOMBSTONE = "review_tombstone";
    private static final String BOOKING_VACANCY_ALERT = "booking_vacancy_alert";

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor;
    private final PhoneVerificationStorePort phoneVerificationStore;
    private final EmailVerificationStorePort emailVerificationStore;
    private final CartMergeRequestStorePort cartMergeRequestStore;
    private final ImageMediaRetentionService imageMediaRetentionService;
    private final NotificationRetentionPort notificationRetentionPort;
    private final AdminAuthHistoryPort adminAuthHistoryPort;
    private final ReviewEvidenceRetentionService reviewEvidenceRetentionService;
    private final ReviewTombstoneRetentionPort reviewTombstoneRetentionPort;
    private final BookingVacancyAlertRetentionPort vacancyAlertRetentionPort;
    private final GuestTokenProperties guestTokenProperties;
    private final Clock clock;
    /** 삭제 묶음마다 독립 커밋하도록 새 트랜잭션을 연다. */
    private final TransactionTemplate requiresNew;

    public DefaultPersonalDataRetentionBatchService(
            PaymentAttemptReaderPort attemptReader,
            PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor,
            PhoneVerificationStorePort phoneVerificationStore,
            EmailVerificationStorePort emailVerificationStore,
            CartMergeRequestStorePort cartMergeRequestStore,
            ImageMediaRetentionService imageMediaRetentionService,
            NotificationRetentionPort notificationRetentionPort,
            AdminAuthHistoryPort adminAuthHistoryPort,
            ReviewEvidenceRetentionService reviewEvidenceRetentionService,
            ReviewTombstoneRetentionPort reviewTombstoneRetentionPort,
            BookingVacancyAlertRetentionPort vacancyAlertRetentionPort,
            GuestTokenProperties guestTokenProperties,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.attemptReader = attemptReader;
        this.attemptCleanupProcessor = attemptCleanupProcessor;
        this.phoneVerificationStore = phoneVerificationStore;
        this.emailVerificationStore = emailVerificationStore;
        this.cartMergeRequestStore = cartMergeRequestStore;
        this.imageMediaRetentionService = imageMediaRetentionService;
        this.notificationRetentionPort = notificationRetentionPort;
        this.adminAuthHistoryPort = adminAuthHistoryPort;
        this.reviewEvidenceRetentionService = reviewEvidenceRetentionService;
        this.reviewTombstoneRetentionPort = reviewTombstoneRetentionPort;
        this.vacancyAlertRetentionPort = vacancyAlertRetentionPort;
        this.guestTokenProperties = guestTokenProperties;
        this.clock = clock;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public BatchResult cleanUpExpiredSensitiveData() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime paymentCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()), ZoneOffset.UTC);
        LocalDateTime verificationCutoff = now.minus(PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY);
        LocalDateTime emailVerificationCutoff =
                now.minus(EMAIL_VERIFICATION_RETENTION_AFTER_EXPIRY);
        LocalDateTime cartMergeCutoff = now.minus(CART_MERGE_REQUEST_RETENTION);
        LocalDateTime notificationCutoff = now.minus(NOTIFICATION_RETENTION);
        LocalDateTime adminAuthHistoryCutoff = now.minus(ADMIN_AUTH_HISTORY_RETENTION);
        LocalDateTime vacancyAlertTerminalCutoff = now.minus(VACANCY_ALERT_TERMINAL_RETENTION);

        BatchResult result = cleanUpPaymentAttempts(paymentCutoff);
        result = result.merge(deleteInBatches(
                PHONE_VERIFICATION,
                inNewTransaction(() -> phoneVerificationStore.deleteExpiredBefore(
                        verificationCutoff, PAGE_SIZE))));
        result = result.merge(deleteInBatches(
                EMAIL_VERIFICATION,
                inNewTransaction(() -> emailVerificationStore.deleteExpiredBefore(
                        emailVerificationCutoff, PAGE_SIZE))));
        result = result.merge(deleteInBatches(
                CART_MERGE_REQUEST,
                inNewTransaction(() -> cartMergeRequestStore.deleteCreatedBefore(
                        cartMergeCutoff, PAGE_SIZE))));
        result = result.merge(isolateCountSource(
                IMAGE_MEDIA,
                imageMediaRetentionService::deleteUnreferencedImages));
        result = result.merge(deleteInBatches(
                NOTIFICATION_LOG,
                inNewTransaction(() -> notificationRetentionPort.deleteChannelLogsBefore(
                        notificationCutoff, PAGE_SIZE))));
        result = result.merge(deleteInBatches(
                NOTIFICATION_OUTBOX,
                inNewTransaction(() -> notificationRetentionPort.deleteTerminalOutboxesBefore(
                        notificationCutoff, PAGE_SIZE))));
        result = result.merge(deleteInBatches(
                ADMIN_AUTH_HISTORY,
                inNewTransaction(() -> adminAuthHistoryPort.deleteCreatedBefore(
                        adminAuthHistoryCutoff, PAGE_SIZE))));
        result = result.merge(deleteInBatches(
                BOOKING_VACANCY_ALERT,
                inNewTransaction(() -> vacancyAlertRetentionPort.deleteExpiredBatch(
                        now, vacancyAlertTerminalCutoff, PAGE_SIZE))));
        // 후기 증거 정리는 자체 트랜잭션과 파일 삭제 순서를 가지므로 바깥 트랜잭션을 열지 않는다.
        result = result.merge(deleteInBatches(
                REVIEW_EVIDENCE,
                () -> reviewEvidenceRetentionService.deleteExpiredBatch(now, PAGE_SIZE)));
        LocalDateTime reviewTombstoneCutoff = now.minus(REVIEW_TOMBSTONE_RETENTION);
        return result.merge(deleteInBatches(
                REVIEW_TOMBSTONE,
                inNewTransaction(() -> reviewTombstoneRetentionPort.deleteUnblockedBefore(
                        reviewTombstoneCutoff, PAGE_SIZE))));
    }

    private IntSupplier inNewTransaction(IntSupplier deleteBatch) {
        return () -> requiresNew.execute(status -> deleteBatch.getAsInt());
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
