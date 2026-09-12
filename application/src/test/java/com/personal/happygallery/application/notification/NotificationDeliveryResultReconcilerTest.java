package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResult;
import com.personal.happygallery.application.notification.port.out.NotificationDeliveryResultProvider;
import com.personal.happygallery.application.notification.port.out.NotificationSendOutcome;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryResultReconcilerTest {

    @ParameterizedTest
    @CsvSource({"SUCCESS, true", "SUCCESS, false", "DELIVERY_UNKNOWN, true", "DELIVERY_UNKNOWN, false"})
    @DisplayName("SMS 전환 후 이력 저장이 실패해도 발송 결과를 보존하고 재발송을 예약하지 않는다")
    void reconcilePending_fallbackAuditFailurePreservesDeliveryResult(
            NotificationSendResult deliveryResult, boolean ownsProcessing) {
        NotificationOutboxTransactionService transactionService =
                mock(NotificationOutboxTransactionService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationDeliveryResultProvider provider = mock(NotificationDeliveryResultProvider.class);
        NotificationDeliveryResultReservation reservation = new NotificationDeliveryResultReservation(
                1L, "processing-token", NotificationRecipientType.USER, null, 10L,
                NotificationEventType.BOOKING_CONFIRMED, "idempotency-key",
                NotificationChannel.KAKAO, "kakao-request-id", 1L);
        when(provider.channel()).thenReturn(NotificationChannel.KAKAO);
        when(transactionService.reserveNextDeliveryResult(1))
                .thenReturn(Optional.of(reservation), Optional.empty());
        when(provider.findResult("kakao-request-id", 1L))
                .thenReturn(NotificationDeliveryResult.failed("MRC02"));
        when(transactionService.markProviderFailedAudit(reservation, "MRC02")).thenReturn(true);
        boolean delivered = deliveryResult == NotificationSendResult.SUCCESS;
        when(notificationService.sendByUserIdWithOutcome(
                10L, NotificationEventType.BOOKING_CONFIRMED, "idempotency-key", NotificationChannel.KAKAO))
                .thenThrow(delivered
                        ? NotificationAuditPersistenceException.afterCompletedDelivery()
                        : NotificationAuditPersistenceException.afterUnknownDelivery());
        if (delivered) {
            when(transactionService.markSentWithAuditFailure(
                    1L, "processing-token", "AUDIT_LOG_PERSISTENCE_FAILED")).thenReturn(ownsProcessing);
        } else {
            when(transactionService.markPermanentFailure(
                    1L, "processing-token", "DELIVERY_RESULT_UNKNOWN:AUDIT_LOG_PERSISTENCE_FAILED"))
                    .thenReturn(ownsProcessing);
        }
        NotificationDeliveryResultReconciler reconciler = new NotificationDeliveryResultReconciler(
                transactionService, notificationService, List.of(provider));

        var result = reconciler.reconcilePending();

        if (delivered) {
            verify(transactionService).markSentWithAuditFailure(
                    1L, "processing-token", "AUDIT_LOG_PERSISTENCE_FAILED");
        } else {
            verify(transactionService).markPermanentFailure(
                    1L, "processing-token", "DELIVERY_RESULT_UNKNOWN:AUDIT_LOG_PERSISTENCE_FAILED");
        }
        verify(transactionService, never()).rescheduleDeliveryResult(anyLong(), anyString(), anyString());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isEqualTo(delivered && ownsProcessing ? 1 : 0);
            softly.assertThat(result.failureCount()).isEqualTo(!delivered && ownsProcessing ? 1 : 0);
        });
    }

    @DisplayName("알림톡 최종 실패를 확인한 뒤에만 SMS 발송 결과 대기로 전환한다")
    @Test
    void reconcilePending_failedAlimtalk_startsSmsFallback() {
        NotificationOutboxTransactionService transactionService =
                mock(NotificationOutboxTransactionService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationDeliveryResultProvider provider = mock(NotificationDeliveryResultProvider.class);
        NotificationDeliveryResultReservation reservation = new NotificationDeliveryResultReservation(
                1L,
                "processing-token",
                NotificationRecipientType.USER,
                null,
                10L,
                NotificationEventType.BOOKING_CONFIRMED,
                "idempotency-key",
                NotificationChannel.KAKAO,
                "kakao-request-id",
                1L);
        when(provider.channel()).thenReturn(NotificationChannel.KAKAO);
        when(transactionService.reserveNextDeliveryResult(1))
                .thenReturn(Optional.of(reservation), Optional.empty());
        when(provider.findResult("kakao-request-id", 1L))
                .thenReturn(NotificationDeliveryResult.failed("MRC02"));
        when(transactionService.markProviderFailedAudit(reservation, "MRC02")).thenReturn(true);
        when(notificationService.sendByUserIdWithOutcome(
                10L,
                NotificationEventType.BOOKING_CONFIRMED,
                "idempotency-key",
                NotificationChannel.KAKAO))
                .thenReturn(NotificationDeliveryAttempt.from(
                        NotificationChannel.SMS,
                        NotificationSendOutcome.accepted("sms-request-id", 2L)));
        when(transactionService.markDeliveryPending(
                1L,
                "processing-token",
                NotificationChannel.SMS,
                "sms-request-id",
                2L,
                null)).thenReturn(true);
        NotificationDeliveryResultReconciler reconciler = new NotificationDeliveryResultReconciler(
                transactionService, notificationService, List.of(provider));

        var result = reconciler.reconcilePending();

        verify(notificationService).sendByUserIdWithOutcome(
                10L,
                NotificationEventType.BOOKING_CONFIRMED,
                "idempotency-key",
                NotificationChannel.KAKAO);
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
        });
    }
}
