package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.application.notification.port.out.ReviewNotificationEligibilityPort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReviewNotificationEligibilityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 9, 10, 0);

    @DisplayName("후기 요청은 주문과 예약 aggregate에 맞는 현재 작성 기회를 조회한다")
    @Test
    void findEligibleRecipient_reviewRequest_routesBySourceAggregate() {
        ReviewNotificationEligibilityPort port = mock(ReviewNotificationEligibilityPort.class);
        ReviewNotificationEligibility eligibility = new ReviewNotificationEligibility(port);
        NotificationReminderRecipient orderRecipient = NotificationReminderRecipient.forUser(10L);
        NotificationReminderRecipient bookingRecipient = NotificationReminderRecipient.forUser(20L);
        when(port.findOrderRequestRecipient(100L)).thenReturn(Optional.of(orderRecipient));
        when(port.findBookingRequestRecipient(200L)).thenReturn(Optional.of(bookingRecipient));

        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_REQUEST, "ORDER", 100L)))
                .contains(orderRecipient);
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_REQUEST, "BOOKING", 200L)))
                .contains(bookingRecipient);
        verify(port).findOrderRequestRecipient(100L);
        verify(port).findBookingRequestRecipient(200L);
    }

    @DisplayName("후기 상태와 공식 답글 알림은 각 현재 상태 조회로 전달한다")
    @Test
    void findEligibleRecipient_reviewState_routesByEvent() {
        ReviewNotificationEligibilityPort port = mock(ReviewNotificationEligibilityPort.class);
        ReviewNotificationEligibility eligibility = new ReviewNotificationEligibility(port);
        NotificationReminderRecipient recipient = NotificationReminderRecipient.forUser(10L);
        when(port.findHiddenReviewRecipient(300L)).thenReturn(Optional.of(recipient));
        when(port.findRepublishedReviewRecipient(300L)).thenReturn(Optional.of(recipient));
        when(port.findOwnerRepliedReviewRecipient(300L)).thenReturn(Optional.of(recipient));

        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_HIDDEN, "REVIEW_MODERATION_ACTION", 300L)))
                .contains(recipient);
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_REPUBLISHED, "REVIEW_MODERATION_ACTION", 300L)))
                .contains(recipient);
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_OWNER_REPLIED, "REVIEW", 300L))).contains(recipient);
        verify(port).findHiddenReviewRecipient(300L);
        verify(port).findRepublishedReviewRecipient(300L);
        verify(port).findOwnerRepliedReviewRecipient(300L);
    }

    @DisplayName("후기 알림의 aggregate가 맞지 않으면 조회하지 않고 일반 알림은 거절한다")
    @Test
    void findEligibleRecipient_invalidAggregateOrEvent_doesNotLookup() {
        ReviewNotificationEligibilityPort port = mock(ReviewNotificationEligibilityPort.class);
        ReviewNotificationEligibility eligibility = new ReviewNotificationEligibility(port);

        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_REQUEST, "REVIEW", 300L))).isEmpty();
        assertThat(eligibility.findEligibleRecipient(
                outbox(NotificationEventType.REVIEW_HIDDEN, "ORDER", 300L))).isEmpty();
        assertThatThrownBy(() -> eligibility.findEligibleRecipient(
                outbox(NotificationEventType.ORDER_PAID, "ORDER", 300L)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(port);
    }

    private static NotificationOutbox outbox(
            NotificationEventType eventType, String aggregateType, Long aggregateId) {
        return NotificationOutbox.from(
                NotificationRequestedEvent.forUser(1L, eventType, aggregateType, aggregateId),
                NOW);
    }
}
