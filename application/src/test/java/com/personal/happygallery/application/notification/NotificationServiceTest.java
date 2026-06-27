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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-27T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

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
