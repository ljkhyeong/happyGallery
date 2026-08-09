package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationOutboxInsertPort;
import com.personal.happygallery.application.notification.port.out.NotificationOutboxPort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewNotificationPreparationTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-09T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @DisplayName("발송 시점에 의미가 사라진 후기 알림은 외부 채널 없이 종결한다")
    @Test
    void prepareDelivery_irrelevantReviewNotification_marksObsolete() {
        NotificationOutbox outbox = reviewOutbox(10L);
        String token = outbox.markProcessing(LocalDateTime.now(CLOCK).minusSeconds(1));
        NotificationOutboxPort outboxPort = mock(NotificationOutboxPort.class);
        NotificationReminderEligibility reminderEligibility = mock(NotificationReminderEligibility.class);
        ReviewNotificationEligibility reviewEligibility = mock(ReviewNotificationEligibility.class);
        when(outboxPort.findByIdForUpdate(outbox.getId())).thenReturn(Optional.of(outbox));
        when(reviewEligibility.findEligibleRecipient(outbox)).thenReturn(Optional.empty());
        NotificationOutboxTransactionService service = service(
                outboxPort, reminderEligibility, reviewEligibility);

        NotificationOutboxDeliveryPreparation preparation =
                service.prepareDelivery(outbox.getId(), token);

        assertSoftly(softly -> {
            softly.assertThat(preparation.status())
                    .isEqualTo(NotificationOutboxPreparationStatus.OBSOLETE);
            softly.assertThat(preparation.delivery()).isNull();
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
            softly.assertThat(outbox.getLastError()).isEqualTo("REVIEW_NO_LONGER_RELEVANT");
        });
        verify(reviewEligibility).findEligibleRecipient(outbox);
        verifyNoInteractions(reminderEligibility);
    }

    @DisplayName("유효한 후기 알림은 현재 후기 작성자를 수신자로 다시 확정한다")
    @Test
    void prepareDelivery_relevantReviewNotification_refreshesCurrentRecipient() {
        NotificationOutbox outbox = reviewOutbox(10L);
        String token = outbox.markProcessing(LocalDateTime.now(CLOCK).minusSeconds(1));
        NotificationOutboxPort outboxPort = mock(NotificationOutboxPort.class);
        NotificationReminderEligibility reminderEligibility = mock(NotificationReminderEligibility.class);
        ReviewNotificationEligibility reviewEligibility = mock(ReviewNotificationEligibility.class);
        when(outboxPort.findByIdForUpdate(outbox.getId())).thenReturn(Optional.of(outbox));
        when(reviewEligibility.findEligibleRecipient(outbox))
                .thenReturn(Optional.of(NotificationReminderRecipient.forUser(20L)));
        NotificationOutboxTransactionService service = service(
                outboxPort, reminderEligibility, reviewEligibility);

        NotificationOutboxDeliveryPreparation preparation =
                service.prepareDelivery(outbox.getId(), token);

        assertSoftly(softly -> {
            softly.assertThat(preparation.status())
                    .isEqualTo(NotificationOutboxPreparationStatus.READY);
            softly.assertThat(preparation.delivery().userId()).isEqualTo(20L);
            softly.assertThat(outbox.getUserId()).isEqualTo(20L);
            softly.assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING);
        });
        verify(reviewEligibility).findEligibleRecipient(outbox);
        verifyNoInteractions(reminderEligibility);
    }

    @DisplayName("종결된 후기 알림은 같은 멱등 요청이 와도 리마인드처럼 다시 열지 않는다")
    @Test
    void enqueue_duplicateReviewNotification_doesNotReactivateObsolete() {
        NotificationOutboxInsertPort insertPort = mock(NotificationOutboxInsertPort.class);
        NotificationOutboxPort outboxPort = mock(NotificationOutboxPort.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        NotificationRequestedEvent event = NotificationRequestedEvent.forUserOncePerAggregate(
                10L, NotificationEventType.REVIEW_REQUEST, "ORDER", 100L);
        when(insertPort.insertIfAbsent(any(NotificationOutbox.class))).thenReturn(false);
        NotificationOutboxService service = new NotificationOutboxService(
                insertPort, outboxPort, eventPublisher, CLOCK);

        boolean enqueued = service.enqueue(event);

        assertSoftly(softly -> softly.assertThat(enqueued).isFalse());
        verifyNoInteractions(outboxPort, eventPublisher);
    }

    private static NotificationOutbox reviewOutbox(Long userId) {
        return NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        userId,
                        NotificationEventType.REVIEW_HIDDEN,
                        "REVIEW_MODERATION_ACTION",
                        30L),
                LocalDateTime.now(CLOCK).minusMinutes(1));
    }

    private static NotificationOutboxTransactionService service(
            NotificationOutboxPort outboxPort,
            NotificationReminderEligibility reminderEligibility,
            ReviewNotificationEligibility reviewEligibility) {
        return new NotificationOutboxTransactionService(
                outboxPort,
                reminderEligibility,
                reviewEligibility,
                mock(ApplicationEventPublisher.class),
                CLOCK);
    }
}
