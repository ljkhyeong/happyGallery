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

    /** 주문 자체를 집계 단위로 고객 알림을 발행한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void notifyCustomer(Order order, NotificationEventType eventType) {
        notifyCustomer(order, eventType, NotificationAggregateType.ORDER, order.getId());
    }

    /** 주문의 회원·비회원 수신자에게 지정한 집계(주문·클레임) 단위로 알림을 발행한다. */
    @Transactional(propagation = Propagation.MANDATORY)
    void notifyCustomer(Order order, NotificationEventType eventType,
                        NotificationAggregateType aggregateType, Long aggregateId) {
        // 지연 안내는 같은 주문에 여러 번 보낼 수 있다.
        NotificationRequestedEvent event = eventType == NotificationEventType.ORDER_DELAY_REQUESTED
                ? NotificationRequestedEvent.forRecipientRepeatable(
                        order.getUserId(), order.getGuestId(), eventType, aggregateType, aggregateId)
                : NotificationRequestedEvent.forRecipient(
                        order.getUserId(), order.getGuestId(), eventType, aggregateType, aggregateId);
        eventPublisher.publishEvent(event);
    }
}
