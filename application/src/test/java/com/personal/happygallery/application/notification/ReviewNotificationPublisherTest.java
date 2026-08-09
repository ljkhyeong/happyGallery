package com.personal.happygallery.application.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ReviewNotificationPublisherTest {

    @DisplayName("후기 작성 요청은 수신자가 바뀌어도 주문·예약 원천당 같은 멱등키를 사용한다")
    @Test
    void requestReview_usesRecipientIndependentAggregateKey() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReviewNotificationPublisher publisher = new ReviewNotificationPublisher(eventPublisher);
        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);

        publisher.requestForOrder(10L, 100L);
        publisher.requestForOrder(20L, 100L);
        publisher.requestForBooking(10L, 200L);

        verify(eventPublisher, times(3)).publishEvent(captor.capture());
        List<NotificationRequestedEvent> events = captor.getAllValues();
        assertSoftly(softly -> {
            softly.assertThat(events)
                    .extracting(NotificationRequestedEvent::eventType)
                    .containsOnly(NotificationEventType.REVIEW_REQUEST);
            softly.assertThat(events.get(0).idempotencyKey())
                    .isEqualTo("AGGREGATE:REVIEW_REQUEST:ORDER:100");
            softly.assertThat(events.get(1).idempotencyKey())
                    .isEqualTo(events.get(0).idempotencyKey());
            softly.assertThat(events.get(2).idempotencyKey())
                    .isEqualTo("AGGREGATE:REVIEW_REQUEST:BOOKING:200");
        });
    }

    @DisplayName("후기 상태는 조치 ID로, 공식 답글은 후기 ID로 알림을 발행한다")
    @Test
    void reviewChanges_publishReviewAggregateEvents() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReviewNotificationPublisher publisher = new ReviewNotificationPublisher(eventPublisher);
        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);

        publisher.publishHidden(10L, 40L);
        publisher.publishRepublished(10L, 50L);
        publisher.publishOwnerReplied(10L, 30L);

        verify(eventPublisher, times(3)).publishEvent(captor.capture());
        assertSoftly(softly -> {
            softly.assertThat(captor.getAllValues())
                    .extracting(NotificationRequestedEvent::eventType)
                    .containsExactly(
                            NotificationEventType.REVIEW_HIDDEN,
                            NotificationEventType.REVIEW_REPUBLISHED,
                            NotificationEventType.REVIEW_OWNER_REPLIED);
            softly.assertThat(captor.getAllValues().get(0).aggregateType())
                    .isEqualTo("REVIEW_MODERATION_ACTION");
            softly.assertThat(captor.getAllValues().get(0).aggregateId()).isEqualTo(40L);
            softly.assertThat(captor.getAllValues().get(1).aggregateType())
                    .isEqualTo("REVIEW_MODERATION_ACTION");
            softly.assertThat(captor.getAllValues().get(1).aggregateId()).isEqualTo(50L);
            softly.assertThat(captor.getAllValues().get(2).aggregateType()).isEqualTo("REVIEW");
            softly.assertThat(captor.getAllValues().get(2).aggregateId()).isEqualTo(30L);
            softly.assertThat(captor.getAllValues().get(0).idempotencyKey())
                    .isEqualTo("USER:10:REVIEW_HIDDEN:REVIEW_MODERATION_ACTION:40");
            softly.assertThat(captor.getAllValues().get(1).idempotencyKey())
                    .isEqualTo("USER:10:REVIEW_REPUBLISHED:REVIEW_MODERATION_ACTION:50");
            softly.assertThat(captor.getAllValues().get(2).idempotencyKey())
                    .isEqualTo("USER:10:REVIEW_OWNER_REPLIED:REVIEW:30");
        });
    }

    @DisplayName("회원이 없는 완료 원천은 후기 알림 요청을 발행하지 않는다")
    @Test
    void requestReview_withoutMember_doesNotPublish() {
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ReviewNotificationPublisher publisher = new ReviewNotificationPublisher(eventPublisher);

        publisher.requestForOrder(null, 100L);
        publisher.requestForBooking(null, 200L);
        publisher.publishHidden(null, 400L);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }
}
