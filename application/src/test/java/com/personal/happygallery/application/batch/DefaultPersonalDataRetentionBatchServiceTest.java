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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

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
        PhoneVerificationStorePort verificationRetention = mock(PhoneVerificationStorePort.class);
        EmailVerificationStorePort emailVerificationRetention =
                mock(EmailVerificationStorePort.class);
        CartMergeRequestStorePort cartRetention = mock(CartMergeRequestStorePort.class);
        ImageMediaRetentionService imageRetention = mock(ImageMediaRetentionService.class);
        NotificationRetentionPort notificationRetention = mock(NotificationRetentionPort.class);
        AdminAuthHistoryPort adminAuthRetention = mock(AdminAuthHistoryPort.class);
        ReviewEvidenceRetentionService reviewEvidenceRetention =
                mock(ReviewEvidenceRetentionService.class);
        ReviewTombstoneRetentionPort reviewTombstoneRetention =
                mock(ReviewTombstoneRetentionPort.class);
        BookingVacancyAlertRetentionPort vacancyAlertRetention =
                mock(BookingVacancyAlertRetentionPort.class);
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
        when(verificationRetention.deleteExpiredBefore(any(), eq(100)))
                .thenThrow(new IllegalArgumentException("verification detail"));
        when(emailVerificationRetention.deleteExpiredBefore(any(), eq(100))).thenReturn(1);
        when(cartRetention.deleteCreatedBefore(any(), eq(100))).thenReturn(1);
        when(imageRetention.deleteUnreferencedImages()).thenReturn(2);
        when(notificationRetention.deleteChannelLogsBefore(any(), eq(100)))
                .thenThrow(new IllegalStateException("notification detail"));
        when(notificationRetention.deleteTerminalOutboxesBefore(any(), eq(100)))
                .thenReturn(3);
        when(adminAuthRetention.deleteCreatedBefore(any(), eq(100))).thenReturn(4);
        when(vacancyAlertRetention.deleteExpiredBatch(any(), any(), eq(100))).thenReturn(2);
        when(reviewTombstoneRetention.deleteUnblockedBefore(any(), eq(100)))
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
                        vacancyAlertRetention,
                        tokenProperties,
                        clock,
                        mock(PlatformTransactionManager.class));

        BatchResult result = service.cleanUpExpiredSensitiveData();

        assertThat(result.successCount()).isEqualTo(14);
        assertThat(result.failureReasons()).containsExactlyInAnyOrderEntriesOf(
                Map.of(
                        "payment_attempt", 2,
                        "phone_verification", 1,
                        "notification_log", 1,
                        "review_tombstone", 1));
        verify(notificationRetention).deleteTerminalOutboxesBefore(any(), eq(100));
        verify(adminAuthRetention).deleteCreatedBefore(any(), eq(100));
        verify(vacancyAlertRetention).deleteExpiredBatch(any(), any(), eq(100));
        verify(reviewEvidenceRetention).deleteExpiredBatch(any(), eq(100));
        verify(reviewTombstoneRetention).deleteUnblockedBefore(
                eq(expectedReviewTombstoneCutoff), eq(100));
        InOrder reviewRetentionOrder = inOrder(
                reviewEvidenceRetention, reviewTombstoneRetention);
        reviewRetentionOrder.verify(reviewEvidenceRetention).deleteExpiredBatch(any(), eq(100));
        reviewRetentionOrder.verify(reviewTombstoneRetention).deleteUnblockedBefore(
                eq(expectedReviewTombstoneCutoff), eq(100));
    }
}
