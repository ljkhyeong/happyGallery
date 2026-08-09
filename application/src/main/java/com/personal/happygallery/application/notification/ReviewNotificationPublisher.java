package com.personal.happygallery.application.notification;

import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 후기 작성 기회와 운영 상태 변경 알림을 도메인 트랜잭션의 outbox 요청으로 발행한다. */
@Component
public class ReviewNotificationPublisher {

    private static final String ORDER = "ORDER";
    private static final String BOOKING = "BOOKING";
    private static final String REVIEW = "REVIEW";
    private static final String REVIEW_MODERATION_ACTION = "REVIEW_MODERATION_ACTION";

    private final ApplicationEventPublisher eventPublisher;

    public ReviewNotificationPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requestForOrder(Long userId, Long orderId) {
        if (userId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequestedEvent.forUserOncePerAggregate(
                userId, NotificationEventType.REVIEW_REQUEST, ORDER, orderId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requestForBooking(Long userId, Long bookingId) {
        if (userId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequestedEvent.forUserOncePerAggregate(
                userId, NotificationEventType.REVIEW_REQUEST, BOOKING, bookingId));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishHidden(Long userId, Long moderationActionId) {
        publishModerationAction(
                userId, moderationActionId, NotificationEventType.REVIEW_HIDDEN);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishRepublished(Long userId, Long moderationActionId) {
        publishModerationAction(
                userId, moderationActionId, NotificationEventType.REVIEW_REPUBLISHED);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOwnerReplied(Long userId, Long reviewId) {
        publishReviewState(userId, reviewId, NotificationEventType.REVIEW_OWNER_REPLIED);
    }

    private void publishReviewState(
            Long userId, Long reviewId, NotificationEventType eventType) {
        if (userId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                userId, eventType, REVIEW, reviewId));
    }

    private void publishModerationAction(
            Long userId,
            Long moderationActionId,
            NotificationEventType eventType) {
        if (userId == null) {
            return;
        }
        eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                userId, eventType, REVIEW_MODERATION_ACTION, moderationActionId));
    }
}
