package com.personal.happygallery.application.batch;

import com.personal.happygallery.application.admin.AdminAuthHistoryRetentionService;
import com.personal.happygallery.application.cart.CartMergeRequestRetentionService;
import com.personal.happygallery.application.customer.EmailVerificationRetentionService;
import com.personal.happygallery.application.customer.PhoneVerificationRetentionService;
import com.personal.happygallery.application.media.ImageMediaRetentionService;
import com.personal.happygallery.application.notification.NotificationRetentionService;
import com.personal.happygallery.application.payment.PaymentAttemptSensitiveDataCleanupProcessor;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.review.ReviewEvidenceRetentionService;
import com.personal.happygallery.application.review.ReviewTombstoneRetentionService;
import com.personal.happygallery.application.token.GuestTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPersonalDataRetentionBatchServiceTest {

    @DisplayName("개인정보 보존 배치는 실패한 소스를 집계하고 나머지 소스를 계속 정리한다")
    @Test
    void cleanUpExpiredSensitiveData_isolatesEachSourceFailure() {
        PaymentAttemptReaderPort attemptReader = mock(PaymentAttemptReaderPort.class);
        PaymentAttemptSensitiveDataCleanupProcessor attemptProcessor =
                mock(PaymentAttemptSensitiveDataCleanupProcessor.class);
        PhoneVerificationRetentionService verificationRetention =
                mock(PhoneVerificationRetentionService.class);
        EmailVerificationRetentionService emailVerificationRetention =
                mock(EmailVerificationRetentionService.class);
        CartMergeRequestRetentionService cartRetention =
                mock(CartMergeRequestRetentionService.class);
        ImageMediaRetentionService imageRetention = mock(ImageMediaRetentionService.class);
        NotificationRetentionService notificationRetention =
                mock(NotificationRetentionService.class);
        AdminAuthHistoryRetentionService adminAuthRetention =
                mock(AdminAuthHistoryRetentionService.class);
        ReviewEvidenceRetentionService reviewEvidenceRetention =
                mock(ReviewEvidenceRetentionService.class);
        ReviewTombstoneRetentionService reviewTombstoneRetention =
                mock(ReviewTombstoneRetentionService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneOffset.UTC);
        LocalDateTime expectedReviewTombstoneCutoff =
                LocalDateTime.of(2026, 1, 30, 0, 0);
        GuestTokenProperties tokenProperties =
                new GuestTokenProperties(
                        "s".repeat(32), "", Duration.ofHours(720), Duration.ofHours(24));
        when(attemptReader.findSensitiveDataCleanupCandidateIds(any(), eq(0L), eq(100)))
                .thenReturn(List.of(1L, 2L));
        when(attemptReader.findSensitiveDataCleanupCandidateIds(any(), eq(2L), eq(100)))
                .thenThrow(new IllegalStateException("payment query detail"));
        when(attemptProcessor.clear(eq(1L), any()))
                .thenThrow(new IllegalStateException("payment detail"));
        when(attemptProcessor.clear(eq(2L), any())).thenReturn(true);
        when(verificationRetention.deleteBatchBefore(any(), eq(100)))
                .thenThrow(new IllegalArgumentException("verification detail"));
        when(emailVerificationRetention.deleteBatchBefore(any(), eq(100))).thenReturn(1);
        when(cartRetention.deleteBatchBefore(any(), eq(100))).thenReturn(1);
        when(imageRetention.deleteUnreferencedImages()).thenReturn(2);
        when(notificationRetention.deleteChannelLogsBefore(any(), eq(100)))
                .thenThrow(new IllegalStateException("notification detail"));
        when(notificationRetention.deleteTerminalOutboxesBefore(any(), eq(100)))
                .thenReturn(3);
        when(adminAuthRetention.deleteBatchBefore(any(), eq(100))).thenReturn(4);
        when(reviewTombstoneRetention.deleteBatchBefore(any(), eq(100)))
                .thenThrow(new IllegalStateException("review tombstone detail"));
        DefaultPersonalDataRetentionBatchService service =
                new DefaultPersonalDataRetentionBatchService(
                        attemptReader,
                        attemptProcessor,
                        verificationRetention,
                        emailVerificationRetention,
                        cartRetention,
                        imageRetention,
                        notificationRetention,
                        adminAuthRetention,
                        reviewEvidenceRetention,
                        reviewTombstoneRetention,
                        tokenProperties,
                        clock);

        BatchResult result = service.cleanUpExpiredSensitiveData();

        assertThat(result.successCount()).isEqualTo(12);
        assertThat(result.failureReasons()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "payment_attempt", 2,
                        "phone_verification", 1,
                        "notification_log", 1,
                        "review_tombstone", 1));
        verify(notificationRetention).deleteTerminalOutboxesBefore(any(), eq(100));
        verify(adminAuthRetention).deleteBatchBefore(any(), eq(100));
        verify(reviewEvidenceRetention).deleteExpiredBatch(any(), eq(100));
        verify(reviewTombstoneRetention).deleteBatchBefore(
                eq(expectedReviewTombstoneCutoff), eq(100));
        InOrder reviewRetentionOrder = inOrder(
                reviewEvidenceRetention, reviewTombstoneRetention);
        reviewRetentionOrder.verify(reviewEvidenceRetention).deleteExpiredBatch(any(), eq(100));
        reviewRetentionOrder.verify(reviewTombstoneRetention).deleteBatchBefore(
                eq(expectedReviewTombstoneCutoff), eq(100));
    }
}
