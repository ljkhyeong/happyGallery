package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.application.monitoring.AppMetrics;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import com.personal.happygallery.domain.time.Clocks;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final String IDEMPOTENCY_KEY = "notification-key";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-27T00:00:00Z"),
            Clocks.SEOUL
    );

    @DisplayName("첫 알림 채널이 성공하면 성공 로그를 남기고 다음 채널은 시도하지 않는다")
    @Test
    void sendToUser_firstChannelSucceeds_savesSuccessLogAndStopsFallback() {
        NotificationSenderPort kakaoSender = mock(NotificationSenderPort.class);
        NotificationSenderPort smsSender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(kakaoSender, smsSender), logStore, guestReader, userReader);
        when(kakaoSender.send(
                IDEMPOTENCY_KEY, "01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED))
                .thenReturn(NotificationSendResult.SUCCESS);
        when(kakaoSender.channel()).thenReturn(NotificationChannel.KAKAO);

        NotificationSendResult result = service.sendToUser(
                10L,
                IDEMPOTENCY_KEY,
                "01012345678",
                "회원",
                NotificationEventType.BOOKING_CONFIRMED
        );

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(kakaoSender).send(
                IDEMPOTENCY_KEY, "01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED);
        verify(logStore).save(captor.capture());
        verifyNoInteractions(smsSender);
        assertSoftly(softly -> {
            NotificationLog saved = captor.getValue();
            softly.assertThat(result).isEqualTo(NotificationSendResult.SUCCESS);
            softly.assertThat(saved.getGuestId()).isNull();
            softly.assertThat(saved.getUserId()).isEqualTo(10L);
            softly.assertThat(saved.getChannel()).isEqualTo(NotificationChannel.KAKAO);
            softly.assertThat(saved.getEventType()).isEqualTo(NotificationEventType.BOOKING_CONFIRMED);
            softly.assertThat(saved.getStatus()).isEqualTo("SUCCESS");
            softly.assertThat(saved.getFailReason()).isNull();
            softly.assertThat(saved.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
        });
    }

    @DisplayName("첫 알림 채널이 영구 거절되어도 실패 로그를 남기고 다음 채널을 시도한다")
    @Test
    void sendToGuest_firstChannelFails_fallsBackAndSavesEachResult() {
        NotificationSenderPort kakaoSender = mock(NotificationSenderPort.class);
        NotificationSenderPort smsSender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(kakaoSender, smsSender), logStore, guestReader, userReader);
        when(kakaoSender.send(IDEMPOTENCY_KEY, "01087654321", "게스트", NotificationEventType.REMINDER_D1))
                .thenReturn(NotificationSendResult.PERMANENT_FAILURE);
        when(kakaoSender.channel()).thenReturn(NotificationChannel.KAKAO);
        when(smsSender.send(IDEMPOTENCY_KEY, "01087654321", "게스트", NotificationEventType.REMINDER_D1))
                .thenReturn(NotificationSendResult.SUCCESS);
        when(smsSender.channel()).thenReturn(NotificationChannel.SMS);

        NotificationSendResult result = service.sendToGuest(
                20L,
                IDEMPOTENCY_KEY,
                "01087654321",
                "게스트",
                NotificationEventType.REMINDER_D1
        );

        var senderOrder = inOrder(kakaoSender, smsSender);
        senderOrder.verify(kakaoSender).send(
                IDEMPOTENCY_KEY, "01087654321", "게스트", NotificationEventType.REMINDER_D1);
        senderOrder.verify(smsSender).send(
                IDEMPOTENCY_KEY, "01087654321", "게스트", NotificationEventType.REMINDER_D1);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logStore, times(2)).save(captor.capture());
        assertSoftly(softly -> {
            NotificationLog kakaoLog = captor.getAllValues().getFirst();
            NotificationLog smsLog = captor.getAllValues().get(1);
            softly.assertThat(result).isEqualTo(NotificationSendResult.SUCCESS);
            softly.assertThat(kakaoLog.getGuestId()).isEqualTo(20L);
            softly.assertThat(kakaoLog.getUserId()).isNull();
            softly.assertThat(kakaoLog.getChannel()).isEqualTo(NotificationChannel.KAKAO);
            softly.assertThat(kakaoLog.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(kakaoLog.getStatus()).isEqualTo("FAILED");
            softly.assertThat(kakaoLog.getFailReason()).isEqualTo("PERMANENT_FAILURE");
            softly.assertThat(kakaoLog.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
            softly.assertThat(smsLog.getGuestId()).isEqualTo(20L);
            softly.assertThat(smsLog.getUserId()).isNull();
            softly.assertThat(smsLog.getChannel()).isEqualTo(NotificationChannel.SMS);
            softly.assertThat(smsLog.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(smsLog.getStatus()).isEqualTo("SUCCESS");
            softly.assertThat(smsLog.getFailReason()).isNull();
            softly.assertThat(smsLog.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
        });
    }

    @DisplayName("알림 발송 예외는 원문 대신 고정 실패 사유로 기록한다")
    @Test
    void sendToUser_senderThrows_savesSanitizedFailureReason() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(sender), logStore, guestReader, userReader);
        when(sender.channel()).thenReturn(NotificationChannel.SMS);
        when(sender.send(IDEMPOTENCY_KEY, "01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED))
                .thenThrow(new IllegalStateException("phone=01012345678 recipient=회원"));

        NotificationSendResult result = service.sendToUser(
                10L,
                IDEMPOTENCY_KEY,
                "01012345678",
                "회원",
                NotificationEventType.BOOKING_CONFIRMED
        );

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logStore).save(captor.capture());
        assertSoftly(softly -> {
            NotificationLog saved = captor.getValue();
            softly.assertThat(result).isEqualTo(NotificationSendResult.TRANSIENT_FAILURE);
            softly.assertThat(saved.getStatus()).isEqualTo("FAILED");
            softly.assertThat(saved.getFailReason()).isEqualTo("DELIVERY_EXCEPTION");
        });
    }

    @DisplayName("회원 수신자를 찾지 못하면 SYSTEM 실패 로그를 남기고 실제 채널 발송은 시도하지 않는다")
    @Test
    void sendByUserId_userNotFound_savesSystemFailureLog() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(sender), logStore, guestReader, userReader);
        when(userReader.findById(10L)).thenReturn(Optional.empty());

        service.sendByUserId(10L, NotificationEventType.PASS_EXPIRY_SOON, IDEMPOTENCY_KEY);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logStore).save(captor.capture());
        verifyNoInteractions(sender);
        assertSoftly(softly -> {
            NotificationLog saved = captor.getValue();
            softly.assertThat(saved.getUserId()).isEqualTo(10L);
            softly.assertThat(saved.getGuestId()).isNull();
            softly.assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SYSTEM);
            softly.assertThat(saved.getEventType()).isEqualTo(NotificationEventType.PASS_EXPIRY_SOON);
            softly.assertThat(saved.getStatus()).isEqualTo("FAILED");
            softly.assertThat(saved.getFailReason()).isEqualTo("RECIPIENT_NOT_FOUND");
            softly.assertThat(saved.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
        });
    }

    @DisplayName("게스트 수신자를 찾지 못하면 SYSTEM 실패 로그를 남기고 실제 채널 발송은 시도하지 않는다")
    @Test
    void sendByGuestId_guestNotFound_savesSystemFailureLog() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(sender), logStore, guestReader, userReader);
        when(guestReader.findById(20L)).thenReturn(Optional.empty());

        service.sendByGuestId(20L, NotificationEventType.REMINDER_D1, IDEMPOTENCY_KEY);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logStore).save(captor.capture());
        verifyNoInteractions(sender);
        assertSoftly(softly -> {
            NotificationLog saved = captor.getValue();
            softly.assertThat(saved.getGuestId()).isEqualTo(20L);
            softly.assertThat(saved.getUserId()).isNull();
            softly.assertThat(saved.getChannel()).isEqualTo(NotificationChannel.SYSTEM);
            softly.assertThat(saved.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(saved.getStatus()).isEqualTo("FAILED");
            softly.assertThat(saved.getFailReason()).isEqualTo("RECIPIENT_NOT_FOUND");
            softly.assertThat(saved.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
        });
    }

    @DisplayName("모든 채널의 영구 실패는 outbox 재시도 없이 최종 실패로 기록한다")
    @Test
    void dispatchPending_permanentFailure_marksFinalFailure() {
        NotificationOutboxTransactionService transactionService =
                mock(NotificationOutboxTransactionService.class);
        NotificationService notificationService = mock(NotificationService.class);
        NotificationOutboxDispatcher dispatcher =
                new NotificationOutboxDispatcher(transactionService, notificationService);
        var reservation = new NotificationOutboxReservation(99L, "processing-token");
        var delivery = new NotificationOutboxDeliveryRequest(
                99L,
                NotificationRecipientType.USER,
                null,
                10L,
                NotificationEventType.BOOKING_CONFIRMED,
                IDEMPOTENCY_KEY);
        when(transactionService.reserveDispatchable(50, 1)).thenReturn(List.of(reservation));
        when(transactionService.loadRequest(99L, "processing-token")).thenReturn(Optional.of(delivery));
        when(notificationService.sendByUserId(
                10L, NotificationEventType.BOOKING_CONFIRMED, IDEMPOTENCY_KEY))
                .thenReturn(NotificationSendResult.PERMANENT_FAILURE);
        when(transactionService.markPermanentFailure(
                99L, "processing-token", "PERMANENT_DELIVERY_FAILURE"))
                .thenReturn(true);

        var result = dispatcher.dispatchPending();

        verify(transactionService).markPermanentFailure(
                99L, "processing-token", "PERMANENT_DELIVERY_FAILURE");
        verify(transactionService, never()).markDeliveryFailed(
                anyLong(), anyString(), anyString(), anyInt());
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isOne();
        });
    }

    @DisplayName("외부 발송 성공 후 감사 로그 저장이 실패해도 outbox를 재발송하지 않고 경고와 함께 완료한다")
    @Test
    void dispatchPending_deliverySucceedsButAuditFails_marksSentWithoutFallback() {
        NotificationSenderPort kakaoSender = mock(NotificationSenderPort.class);
        NotificationSenderPort smsSender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        AppMetrics metrics = mock(AppMetrics.class);
        NotificationOutboxTransactionService transactionService =
                mock(NotificationOutboxTransactionService.class);
        NotificationService service = new NotificationService(
                List.of(kakaoSender, smsSender),
                logStore,
                guestReader,
                userReader,
                mock(GuestPersonalDataProtector.class),
                metrics,
                CLOCK);
        NotificationOutboxDispatcher dispatcher =
                new NotificationOutboxDispatcher(transactionService, service);
        User user = new User("audit@example.com", "hash", "회원", "01012345678");
        var reservation = new NotificationOutboxReservation(99L, "processing-token");
        when(transactionService.reserveDispatchable(50, 1)).thenReturn(List.of(reservation));
        when(transactionService.loadRequest(99L, "processing-token")).thenReturn(Optional.of(
                new NotificationOutboxDeliveryRequest(
                        99L,
                        NotificationRecipientType.USER,
                        null,
                        10L,
                        NotificationEventType.BOOKING_CONFIRMED,
                        IDEMPOTENCY_KEY)));
        when(userReader.findById(10L)).thenReturn(Optional.of(user));
        when(kakaoSender.channel()).thenReturn(NotificationChannel.KAKAO);
        when(kakaoSender.send(IDEMPOTENCY_KEY, "01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED))
                .thenReturn(NotificationSendResult.SUCCESS);
        when(transactionService.markSentWithAuditFailure(
                99L, "processing-token", "AUDIT_LOG_PERSISTENCE_FAILED"))
                .thenReturn(true);
        when(logStore.save(any(NotificationLog.class)))
                .thenThrow(new IllegalStateException("database unavailable"));

        var result = dispatcher.dispatchPending();

        verify(transactionService).markSentWithAuditFailure(
                99L, "processing-token", "AUDIT_LOG_PERSISTENCE_FAILED");
        verify(transactionService, never()).markDeliveryFailed(
                anyLong(), anyString(), anyString(), anyInt());
        verifyNoInteractions(smsSender);
        verify(metrics).incrementNotificationLogPersistenceFailure();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isOne();
            softly.assertThat(result.failureCount()).isZero();
        });
    }

    private static NotificationService service(List<NotificationSenderPort> senders,
                                               NotificationLogStorePort logStore,
                                               GuestReaderPort guestReader,
                                               UserReaderPort userReader) {
        return new NotificationService(
                senders,
                logStore,
                guestReader,
                userReader,
                mock(GuestPersonalDataProtector.class),
                mock(AppMetrics.class),
                CLOCK
        );
    }
}
