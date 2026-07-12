package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.customer.GuestPhoneProtector;
import com.personal.happygallery.application.customer.port.out.GuestReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.port.out.NotificationLogStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationSenderPort;
import com.personal.happygallery.domain.notification.NotificationChannel;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-27T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
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
        when(kakaoSender.send("01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED)).thenReturn(true);
        when(kakaoSender.channel()).thenReturn(NotificationChannel.KAKAO);

        boolean sent = service.sendToUser(
                10L,
                "01012345678",
                "회원",
                NotificationEventType.BOOKING_CONFIRMED
        );

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(kakaoSender).send("01012345678", "회원", NotificationEventType.BOOKING_CONFIRMED);
        verify(logStore).save(captor.capture());
        verifyNoInteractions(smsSender);
        assertSoftly(softly -> {
            NotificationLog saved = captor.getValue();
            softly.assertThat(sent).isTrue();
            softly.assertThat(saved.getGuestId()).isNull();
            softly.assertThat(saved.getUserId()).isEqualTo(10L);
            softly.assertThat(saved.getChannel()).isEqualTo(NotificationChannel.KAKAO);
            softly.assertThat(saved.getEventType()).isEqualTo(NotificationEventType.BOOKING_CONFIRMED);
            softly.assertThat(saved.getStatus()).isEqualTo("SUCCESS");
            softly.assertThat(saved.getFailReason()).isNull();
            softly.assertThat(saved.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 27, 9, 0));
        });
    }

    @DisplayName("첫 알림 채널이 실패하면 실패 로그를 남기고 다음 채널 성공까지 이어서 시도한다")
    @Test
    void sendToGuest_firstChannelFails_fallsBackAndSavesEachResult() {
        NotificationSenderPort kakaoSender = mock(NotificationSenderPort.class);
        NotificationSenderPort smsSender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(kakaoSender, smsSender), logStore, guestReader, userReader);
        when(kakaoSender.send("01087654321", "게스트", NotificationEventType.REMINDER_D1)).thenReturn(false);
        when(kakaoSender.channel()).thenReturn(NotificationChannel.KAKAO);
        when(smsSender.send("01087654321", "게스트", NotificationEventType.REMINDER_D1)).thenReturn(true);
        when(smsSender.channel()).thenReturn(NotificationChannel.SMS);

        boolean sent = service.sendToGuest(
                20L,
                "01087654321",
                "게스트",
                NotificationEventType.REMINDER_D1
        );

        var senderOrder = inOrder(kakaoSender, smsSender);
        senderOrder.verify(kakaoSender).send("01087654321", "게스트", NotificationEventType.REMINDER_D1);
        senderOrder.verify(smsSender).send("01087654321", "게스트", NotificationEventType.REMINDER_D1);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logStore, times(2)).save(captor.capture());
        assertSoftly(softly -> {
            NotificationLog kakaoLog = captor.getAllValues().get(0);
            NotificationLog smsLog = captor.getAllValues().get(1);
            softly.assertThat(sent).isTrue();
            softly.assertThat(kakaoLog.getGuestId()).isEqualTo(20L);
            softly.assertThat(kakaoLog.getUserId()).isNull();
            softly.assertThat(kakaoLog.getChannel()).isEqualTo(NotificationChannel.KAKAO);
            softly.assertThat(kakaoLog.getEventType()).isEqualTo(NotificationEventType.REMINDER_D1);
            softly.assertThat(kakaoLog.getStatus()).isEqualTo("FAILED");
            softly.assertThat(kakaoLog.getFailReason()).isEqualTo("발송 실패");
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

    @DisplayName("회원 수신자를 찾지 못하면 SYSTEM 실패 로그를 남기고 실제 채널 발송은 시도하지 않는다")
    @Test
    void sendByUserId_userNotFound_savesSystemFailureLog() {
        NotificationSenderPort sender = mock(NotificationSenderPort.class);
        NotificationLogStorePort logStore = mock(NotificationLogStorePort.class);
        GuestReaderPort guestReader = mock(GuestReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        NotificationService service = service(List.of(sender), logStore, guestReader, userReader);
        when(userReader.findById(10L)).thenReturn(Optional.empty());

        service.sendByUserId(10L, NotificationEventType.PASS_EXPIRY_SOON);

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

        service.sendByGuestId(20L, NotificationEventType.REMINDER_D1);

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

    private static NotificationService service(List<NotificationSenderPort> senders,
                                               NotificationLogStorePort logStore,
                                               GuestReaderPort guestReader,
                                               UserReaderPort userReader) {
        return new NotificationService(
                senders,
                logStore,
                guestReader,
                userReader,
                mock(GuestPhoneProtector.class),
                CLOCK
        );
    }
}
