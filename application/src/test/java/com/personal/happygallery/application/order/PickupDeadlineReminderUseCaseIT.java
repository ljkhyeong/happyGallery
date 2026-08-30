package com.personal.happygallery.application.order;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.out.GuestStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.NotificationOutboxDispatcher;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.order.port.in.PickupDeadlineReminderBatchUseCase;
import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationLog;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.booking.Guest;
import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.NotificationLogProbe;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.personal.happygallery.support.NotificationLogTestHelper.awaitLogCount;
import static com.personal.happygallery.support.TestFixtures.accessToken;
import static com.personal.happygallery.support.TestFixtures.guest;
import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class PickupDeadlineReminderUseCaseIT {

    @Autowired PickupDeadlineReminderBatchUseCase reminderBatchService;
    @Autowired NotificationOutboxDispatcher notificationOutboxDispatcher;
    @Autowired OrderStorePort orderStorePort;
    @Autowired FulfillmentPort fulfillmentPort;
    @Autowired GuestStorePort guestStorePort;
    @Autowired UserStorePort userStorePort;
    @Autowired NotificationQueryUseCase notificationQueryUseCase;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired NotificationLogProbe notificationLogProbe;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingWithPassAndRefundData();
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

    @DisplayName("발송 전에 픽업 완료된 주문의 마감 알림은 외부 발송 없이 종결한다")
    @Test
    void dispatchPickupReminder_afterPickupCompletion_marksObsolete() {
        User user = userStorePort.save(new User(
                "pickup-reminder-obsolete@example.com", "hashed-password", "회원", "01078787878"));
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = Order.forMember(user.getId(), 35_000L, now, now.plusDays(1));
        order.approve();
        order.markPickupReady();
        order = orderStorePort.saveAndFlush(order);
        Fulfillment fulfillment = Fulfillment.pickup(order.getId());
        fulfillment.setPickupDeadline(now.plusHours(1));
        fulfillmentPort.save(fulfillment);
        NotificationOutbox outbox = notificationOutboxRepository.save(NotificationOutbox.from(
                NotificationRequestedEvent.forUserOncePerAggregate(
                        user.getId(),
                        NotificationEventType.PICKUP_DEADLINE_REMINDER,
                        "ORDER",
                        order.getId()),
                now));
        order.confirmPickup();
        orderStorePort.saveAndFlush(order);

        BatchResult result = notificationOutboxDispatcher.dispatchPending();

        NotificationOutbox obsolete = notificationOutboxRepository.findById(outbox.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(result.successCount()).isZero();
            softly.assertThat(result.failureCount()).isZero();
            softly.assertThat(obsolete.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
            softly.assertThat(obsolete.getLastError()).isEqualTo("REMINDER_NO_LONGER_ELIGIBLE");
            softly.assertThat(notificationLogProbe.all()).isEmpty();
        });
    }

    @DisplayName("비회원 주문의 픽업 리마인드는 발송 준비 전에 회원 귀속되면 현재 회원에게만 남는다")
    @Test
    void dispatchPickupReminder_afterOrderClaim_refreshesLogAndInboxOwner() {
        LocalDateTime now = LocalDateTime.now(clock);
        Guest previousOwner = guestStorePort.save(guest("비회원 주문자", "01089898989"));
        User currentOwner = userStorePort.save(new User(
                "claimed-pickup@example.com", "hashed-password", "귀속 회원", "01089898989"));
        Order order = Order.forGuest(
                previousOwner.getId(), accessToken(), 35_000L, now, now.plusDays(1));
        order.approve();
        order.markPickupReady();
        order = orderStorePort.saveAndFlush(order);
        Fulfillment fulfillment = Fulfillment.pickup(order.getId());
        fulfillment.setPickupDeadline(now.plusHours(1));
        fulfillmentPort.save(fulfillment);
        NotificationOutbox outbox = notificationOutboxRepository.saveAndFlush(
                NotificationOutbox.from(
                        NotificationRequestedEvent.forGuestOncePerAggregate(
                                previousOwner.getId(),
                                NotificationEventType.PICKUP_DEADLINE_REMINDER,
                                "ORDER",
                                order.getId()),
                        now));
        order.claimToUser(currentOwner.getId());
        orderStorePort.saveAndFlush(order);

        NotificationOutbox sent = await()
                .atMost(5, TimeUnit.SECONDS)
                .until(
                        () -> {
                            notificationOutboxDispatcher.dispatchPending();
                            return notificationOutboxRepository.findById(outbox.getId());
                        },
                        candidate -> candidate
                                .filter(value -> value.getStatus() == NotificationOutboxStatus.SENT)
                                .isPresent())
                .orElseThrow();
        List<NotificationLog> matchingLogs = notificationLogProbe.all().stream()
                .filter(candidate -> candidate.getEventType()
                        == NotificationEventType.PICKUP_DEADLINE_REMINDER)
                .filter(candidate -> currentOwner.getId().equals(candidate.getUserId()))
                .filter(candidate -> candidate.getGuestId() == null)
                .toList();
        var currentOwnerInbox = notificationQueryUseCase.listNotifications(
                currentOwner.getId(), null, 0, 20);
        var previousOwnerInbox = notificationQueryUseCase.listNotifications(
                null, previousOwner.getId(), 0, 20);

        assertSoftly(softly -> {
            softly.assertThat(sent.getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
            softly.assertThat(sent.getRecipientType()).isEqualTo(NotificationRecipientType.USER);
            softly.assertThat(sent.getUserId()).isEqualTo(currentOwner.getId());
            softly.assertThat(sent.getGuestId()).isNull();
            softly.assertThat(matchingLogs).singleElement()
                    .satisfies(log -> {
                        softly.assertThat(log.getEventType())
                                .isEqualTo(NotificationEventType.PICKUP_DEADLINE_REMINDER);
                        softly.assertThat(log.getUserId()).isEqualTo(currentOwner.getId());
                        softly.assertThat(log.getGuestId()).isNull();
                    });
            softly.assertThat(currentOwnerInbox).singleElement()
                    .satisfies(item -> softly.assertThat(item.id()).isEqualTo(outbox.getId()));
            softly.assertThat(previousOwnerInbox).isEmpty();
            softly.assertThat(notificationQueryUseCase.countUnread(currentOwner.getId(), null)).isOne();
            softly.assertThat(notificationQueryUseCase.countUnread(null, previousOwner.getId())).isZero();
        });
    }
}
