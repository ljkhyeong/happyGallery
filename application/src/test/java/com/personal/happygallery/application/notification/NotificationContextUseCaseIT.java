package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.product.port.out.RestockAlertPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.RestockAlert;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.readyStockProduct;
import static com.personal.happygallery.support.TestFixtures.slot;
import static org.assertj.core.api.Assertions.assertThat;

@UseCaseIT
class NotificationContextUseCaseIT {
    @Autowired NotificationQueryUseCase query;
    @Autowired NotificationOutboxRepository outboxRepository;
    @Autowired OrderStorePort orderStore;
    @Autowired OrderItemPort orderItems;
    @Autowired ProductStorePort products;
    @Autowired RestockAlertPort restockAlerts;
    @Autowired UserStorePort users;
    @Autowired BookingStorePort bookings;
    @Autowired ClassStorePort classes;
    @Autowired SlotStorePort slots;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TestCleanupSupport cleanup;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanup.clearNotificationLogs();
        cleanup.clearOrderData();
        cleanup.clearBookingWithPassAndRefundData();
        cleanup.clearUsers();
    }

    @Test
    @DisplayName("알림에는 구매 당시 상품명과 현재 예약일 및 재입고 옵션을 표시한다")
    void inbox_includesOwnedOrderBookingAndRestockDetails() {
        var now = LocalDateTime.now(clock).withNano(0);
        var user = users.save(new User("notification-context@example.com", "hash", "회원", "01098760001"));
        var product = products.save(readyStockProduct("현재 상품명", 10_000L));
        var second = products.save(readyStockProduct("다른 상품", 10_000L));
        var order = orderStore.save(Order.forMember(user.getId(), 20_000L, now, now.plusDays(1)));
        orderItems.save(new OrderItem(order, product.getId(), "구매 당시 작품명", 1, 10_000L));
        orderItems.save(new OrderItem(order, second.getId(), "다른 작품명", 1, 10_000L));
        var bookingClass = classes.save(bookingClass("가죽공예", "CRAFT", 120, 50_000L, 30));
        var firstSlot = slots.save(slot(bookingClass, now.plusDays(2), now.plusDays(2).plusHours(2)));
        var nextSlot = slots.save(slot(bookingClass, now.plusDays(3), now.plusDays(3).plusHours(2)));
        var booking = bookings.save(Booking.forMemberDeposit(user, firstSlot, 5_000L, 45_000L, DepositPaymentMethod.CARD));
        var alert = restockAlerts.saveAndFlush(new RestockAlert(user.getId(), product.getId(), null, "기본 옵션", now));
        var orderNotice = sent(user.getId(), NotificationEventType.ORDER_PAID, "ORDER", order.getId());
        var bookingNotice = sent(user.getId(), NotificationEventType.BOOKING_CONFIRMED, "BOOKING", booking.getId());
        var restockNotice = sent(user.getId(), NotificationEventType.PRODUCT_RESTOCK_AVAILABLE, "RESTOCK_ALERT", alert.getId());
        booking.reschedule(nextSlot);
        bookings.save(booking);

        var results = query.listNotifications(user.getId(), null, 0, 20, false);
        assertThat(results).filteredOn(row -> row.id().equals(orderNotice)).singleElement()
                .satisfies(row -> {
                    assertThat(row.contextTitle()).isEqualTo("주문 #" + order.getId() + " · 구매 당시 작품명 외 1건");
                    assertThat(row.scheduledAt()).isNull();
                });
        assertThat(results).filteredOn(row -> row.id().equals(bookingNotice)).singleElement()
                .satisfies(row -> {
                    assertThat(row.contextTitle()).isEqualTo("예약 #" + booking.getId() + " · 가죽공예");
                    assertThat(row.scheduledAt()).isEqualTo(nextSlot.getStartAt().withNano(0));
                });
        assertThat(results).filteredOn(row -> row.id().equals(restockNotice)).singleElement()
                .extracting(NotificationQueryUseCase.NotificationView::contextTitle)
                .isEqualTo("재입고 · 현재 상품명 · 기본 옵션");
    }

    @Test
    @DisplayName("알림 원본이 없거나 타인 소유이면 관련 내용은 비우고 본인 알림만 반환한다")
    void inbox_omitsMissingAndForeignAggregateDetails() {
        var now = LocalDateTime.now(clock).withNano(0);
        var owner = users.save(new User("notification-owner@example.com", "hash", "회원", "01098760002"));
        var other = users.save(new User("notification-other@example.com", "hash", "다른 회원", "01098760003"));
        var product = products.save(readyStockProduct("타인 작품", 10_000L));
        var order = orderStore.save(Order.forMember(other.getId(), 10_000L, now, now.plusDays(1)));
        orderItems.save(new OrderItem(order, product.getId(), "타인 구매 내용", 1, 10_000L));
        var foreign = sent(owner.getId(), NotificationEventType.ORDER_PAID, "ORDER", order.getId());
        var missing = sent(owner.getId(), NotificationEventType.ORDER_PAID, "ORDER", Long.MAX_VALUE);
        sent(other.getId(), NotificationEventType.ORDER_PAID, "ORDER", order.getId());
        var results = query.listNotifications(owner.getId(), null, 0, 20, false);
        assertThat(results).extracting(NotificationQueryUseCase.NotificationView::id)
                .containsExactlyInAnyOrder(foreign, missing);
        assertThat(results).allSatisfy(row -> {
            assertThat(row.contextTitle()).isNull();
            assertThat(row.scheduledAt()).isNull();
        });
    }

    private Long sent(Long userId, NotificationEventType eventType, String aggregateType, Long aggregateId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var now = LocalDateTime.now(clock).withNano(0);
            var outbox = outboxRepository.save(NotificationOutbox.from(
                    NotificationRequestedEvent.forUser(userId, eventType, aggregateType, aggregateId), now));
            outbox.markSent(outbox.markProcessing(now), now);
            return outbox.getId();
        });
    }
}
