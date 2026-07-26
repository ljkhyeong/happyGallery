package com.personal.happygallery.application.order;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PickupDeadlineReminderUseCaseIT {

    @Autowired PickupDeadlineReminderBatchUseCase reminderBatchService;
    @Autowired OrderStorePort orderStorePort;
    @Autowired FulfillmentPort fulfillmentPort;
    @Autowired UserStorePort userStorePort;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearOrderData();
        cleanupSupport.clearUsers();
    }

    @DisplayName("구형 수신자 기반 outbox가 있으면 픽업 마감 알림을 다시 요청하지 않는다")
    @Test
    void sendPickupDeadlineReminders_legacyRecipientKeyExists_skipsAggregate() {
        User user = userStorePort.save(new User(
                "pickup-reminder-legacy@example.com", "hashed-password", "회원", "01067676767"));
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = Order.forMember(
                user.getId(), 35_000L, now, now.plusDays(1));
        order.approve();
        order.markPickupReady();
        order = orderStorePort.saveAndFlush(order);
        Fulfillment fulfillment = Fulfillment.pickup(order.getId());
        fulfillment.setPickupDeadline(now.plusHours(1));
        fulfillmentPort.save(fulfillment);
        NotificationOutbox legacyOutbox = NotificationOutbox.from(
                NotificationRequestedEvent.forUser(
                        user.getId(),
                        NotificationEventType.PICKUP_DEADLINE_REMINDER,
                        "ORDER",
                        order.getId()),
                now);
        String processingToken = legacyOutbox.markProcessing(now);
        legacyOutbox.markSent(processingToken, now);
        notificationOutboxRepository.saveAndFlush(legacyOutbox);

        BatchResult result = reminderBatchService.sendPickupDeadlineReminders();

        Long orderId = order.getId();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(notificationOutboxRepository.findAll())
                    .filteredOn(outbox ->
                            outbox.getEventType() == NotificationEventType.PICKUP_DEADLINE_REMINDER
                                    && orderId.equals(outbox.getAggregateId()))
                    .singleElement()
                    .satisfies(outbox -> softly.assertThat(outbox.getIdempotencyKey())
                            .startsWith("USER:"));
        });
    }
}
