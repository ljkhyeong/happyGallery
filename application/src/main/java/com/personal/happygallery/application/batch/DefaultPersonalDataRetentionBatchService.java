package com.personal.happygallery.application.batch;

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
import org.springframework.stereotype.Service;

@Service
public class DefaultPersonalDataRetentionBatchService implements PersonalDataRetentionBatchUseCase {

    public static final Duration PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY = Duration.ofDays(1);
    public static final Duration CART_MERGE_REQUEST_RETENTION = Duration.ofDays(7);
    public static final Duration NOTIFICATION_RETENTION = Duration.ofDays(180);
    private static final int PAGE_SIZE = 100;

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor;
    private final PhoneVerificationRetentionService verificationRetentionService;
    private final CartMergeRequestRetentionService cartMergeRequestRetentionService;
    private final ImageMediaRetentionService imageMediaRetentionService;
    private final NotificationRetentionService notificationRetentionService;
    private final GuestTokenProperties guestTokenProperties;
    private final Clock clock;

    public DefaultPersonalDataRetentionBatchService(
            PaymentAttemptReaderPort attemptReader,
            PaymentAttemptSensitiveDataCleanupProcessor attemptCleanupProcessor,
            PhoneVerificationRetentionService verificationRetentionService,
            CartMergeRequestRetentionService cartMergeRequestRetentionService,
            ImageMediaRetentionService imageMediaRetentionService,
            NotificationRetentionService notificationRetentionService,
            GuestTokenProperties guestTokenProperties,
            Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptCleanupProcessor = attemptCleanupProcessor;
        this.verificationRetentionService = verificationRetentionService;
        this.cartMergeRequestRetentionService = cartMergeRequestRetentionService;
        this.imageMediaRetentionService = imageMediaRetentionService;
        this.notificationRetentionService = notificationRetentionService;
        this.guestTokenProperties = guestTokenProperties;
        this.clock = clock;
    }

    @Override
    public BatchResult cleanUpExpiredSensitiveData() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime paymentCutoff = LocalDateTime.ofInstant(
                clock.instant().minus(guestTokenProperties.accessExpiry()), ZoneOffset.UTC);
        BatchResult paymentResult = BatchExecutor.executeByIdCursor(
                afterId -> attemptReader.findSensitiveDataCleanupCandidateIds(
                        paymentCutoff, afterId, PAGE_SIZE),
                attemptId -> attemptId,
                attemptId -> attemptCleanupProcessor.clear(attemptId, paymentCutoff),
                "결제 개인정보 정리");

        LocalDateTime verificationCutoff = now.minus(PHONE_VERIFICATION_RETENTION_AFTER_EXPIRY);
        int deletedVerificationCount = verificationRetentionService.deleteExpiredBefore(verificationCutoff);
        LocalDateTime cartMergeCutoff = now.minus(CART_MERGE_REQUEST_RETENTION);
        int deletedCartMergeRequestCount = deleteCartMergeRequests(cartMergeCutoff);
        int deletedImageCount = imageMediaRetentionService.deleteUnreferencedImages();
        LocalDateTime notificationCutoff = now.minus(NOTIFICATION_RETENTION);
        int deletedNotificationCount = deleteNotifications(notificationCutoff);
        int deletedCount = Math.addExact(deletedVerificationCount, deletedCartMergeRequestCount);
        deletedCount = Math.addExact(deletedCount, deletedImageCount);
        deletedCount = Math.addExact(deletedCount, deletedNotificationCount);
        return paymentResult.merge(BatchResult.successOnly(deletedCount));
    }

    private int deleteCartMergeRequests(LocalDateTime cutoff) {
        int total = 0;
        int deleted;
        do {
            deleted = cartMergeRequestRetentionService.deleteBatchBefore(cutoff, PAGE_SIZE);
            total = Math.addExact(total, deleted);
        } while (deleted == PAGE_SIZE);
        return total;
    }

    private int deleteNotifications(LocalDateTime cutoff) {
        int total = 0;
        int deleted;
        do {
            deleted = notificationRetentionService.deleteChannelLogsBefore(cutoff, PAGE_SIZE);
            total = Math.addExact(total, deleted);
        } while (deleted == PAGE_SIZE);
        do {
            deleted = notificationRetentionService.deleteTerminalOutboxesBefore(cutoff, PAGE_SIZE);
            total = Math.addExact(total, deleted);
        } while (deleted == PAGE_SIZE);
        return total;
    }
}
