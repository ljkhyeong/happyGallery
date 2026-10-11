package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class OrderNotificationSupport {

    private final ApplicationEventPublisher eventPublisher;

    OrderNotificationSupport(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void notifyCustomer(Order order, NotificationEventType eventType) {
        boolean repeatable = eventType == NotificationEventType.ORDER_DELAY_REQUESTED;
        NotificationRequestedEvent event;
        if (order.getUserId() != null) {
            event = repeatable
                    ? NotificationRequestedEvent.forUserRepeatable(
                            order.getUserId(), eventType, NotificationAggregateType.ORDER, order.getId())
                    : NotificationRequestedEvent.forUser(
                            order.getUserId(), eventType, NotificationAggregateType.ORDER, order.getId());
        } else {
            event = repeatable
                    ? NotificationRequestedEvent.forGuestRepeatable(
                            order.getGuestId(), eventType, NotificationAggregateType.ORDER, order.getId())
                    : NotificationRequestedEvent.forGuest(
                            order.getGuestId(), eventType, NotificationAggregateType.ORDER, order.getId());
        }
        eventPublisher.publishEvent(event);
    }
}
