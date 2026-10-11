package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.application.booking.port.out.BookingStorePort;
import com.personal.happygallery.application.booking.port.out.BookingVacancyAlertPort;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase.NotificationTarget;
import com.personal.happygallery.application.notification.port.in.NotificationQueryUseCase.NotificationView;
import com.personal.happygallery.application.order.port.out.OrderClaimPort;
import com.personal.happygallery.application.order.port.out.OrderItemPort;
import com.personal.happygallery.application.order.port.out.OrderStorePort;
import com.personal.happygallery.application.payment.port.out.RefundPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.application.product.port.out.RestockAlertPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingVacancyAlert;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimType;
import com.personal.happygallery.domain.product.RestockAlert;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.personal.happygallery.support.TestFixtures.bookingClass;
import static com.personal.happygallery.support.TestFixtures.memberOrder;
import static com.personal.happygallery.support.TestFixtures.readyStockOrderItem;
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
    @Autowired OrderClaimPort orderClaims;
    @Autowired RefundPort refunds;
    @Autowired BookingVacancyAlertPort vacancyAlerts;
    @Autowired ProductQnaStorePort productQnas;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired TestCleanupSupport cleanup;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "product_qna");
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
        var order = orderStore.save(memberOrder(user.getId(), 20_000L, now, now.plusDays(1)));
        orderItems.save(readyStockOrderItem(order, product.getId(), "구매 당시 작품명", 1, 10_000L));
        orderItems.save(readyStockOrderItem(order, second.getId(), "다른 작품명", 1, 10_000L));
        var bookingClass = classes.save(bookingClass("가죽공예", "CRAFT", 120, 50_000L, 30));
        var firstSlot = slots.save(slot(bookingClass, now.plusDays(2), now.plusDays(2).plusHours(2)));
        var nextSlot = slots.save(slot(bookingClass, now.plusDays(3), now.plusDays(3).plusHours(2)));
        var booking = bookings.save(Booking.forMemberDeposit(user, firstSlot, 5_000L, 45_000L, DepositPaymentMethod.CARD));
        var alert = restockAlerts.saveAndFlush(new RestockAlert(user.getId(), product.getId(), null, "기본 옵션", now));
        var orderNotice = sent(user.getId(), NotificationEventType.ORDER_PAID, NotificationAggregateType.ORDER, order.getId());
        var bookingNotice = sent(user.getId(), NotificationEventType.BOOKING_CONFIRMED, NotificationAggregateType.BOOKING, booking.getId());
        var restockNotice = sent(user.getId(), NotificationEventType.PRODUCT_RESTOCK_AVAILABLE, NotificationAggregateType.RESTOCK_ALERT, alert.getId());
        booking.reschedule(nextSlot);
        bookings.save(booking);

        var results = query.listNotifications(user.getId(), 0, 20, false);
        assertThat(results).filteredOn(row -> row.id().equals(orderNotice)).singleElement()
                .satisfies(row -> {
                    assertThat(row.contextTitle()).isEqualTo("주문 #" + order.getId() + " · 구매 당시 작품명 외 1건");
                    assertThat(row.scheduledAt()).isNull();
                    assertThat(row.target()).isEqualTo(new NotificationTarget(order.getId(), null, null, null, null, null));
                });
        assertThat(results).filteredOn(row -> row.id().equals(bookingNotice)).singleElement()
                .satisfies(row -> {
                    assertThat(row.contextTitle()).isEqualTo("예약 #" + booking.getId() + " · 가죽공예");
                    assertThat(row.scheduledAt()).isEqualTo(nextSlot.getStartAt().withNano(0));
                    assertThat(row.target()).isEqualTo(new NotificationTarget(null, booking.getId(), null, null, null, null));
                });
        assertThat(results).filteredOn(row -> row.id().equals(restockNotice)).singleElement()
                .satisfies(row -> {
                    assertThat(row.contextTitle()).isEqualTo("재입고 · 현재 상품명 · 기본 옵션");
                    assertThat(row.target()).isEqualTo(new NotificationTarget(null, null, product.getId(), null, null, null));
                });
    }

    @Test
    @DisplayName("클레임·환불 알림은 원래 주문·예약으로, 빈자리·상품 문의 알림은 회차·상품으로 잇는다")
    void inbox_resolvesClaimRefundVacancyAndQnaTargets() {
        var now = LocalDateTime.now(clock).withNano(0);
        var user = users.save(new User("notification-target@example.com", "hash", "회원", "01098760004"));
        var product = products.save(readyStockProduct("문의한 작품", 10_000L));
        var order = orderStore.save(memberOrder(user.getId(), 10_000L, now, now.plusDays(1)));
        orderItems.save(readyStockOrderItem(order, product.getId(), "구매 당시 작품명", 1, 10_000L));
        var claim = orderClaims.save(OrderClaim.request(
                order.getId(), OrderClaimType.DAMAGED, OrderClaimResolution.REFUND, "파손", now));
        var bookingClass = classes.save(bookingClass("가죽공예", "CRAFT", 120, 50_000L, 30));
        var bookedSlot = slots.save(slot(bookingClass, now.plusDays(2), now.plusDays(2).plusHours(2)));
        var fullSlot = slots.save(slot(bookingClass, now.plusDays(4), now.plusDays(4).plusHours(2)));
        var booking = bookings.save(Booking.forMemberDeposit(user, bookedSlot, 5_000L, 45_000L, DepositPaymentMethod.CARD));
        var refund = refunds.save(Refund.forBooking(booking, 5_000L));
        var vacancy = vacancyAlerts.save(BookingVacancyAlert.forUser(fullSlot, user.getId()));
        var qna = productQnas.save(new ProductQna(product.getId(), user.getId(), "배송 문의", "언제 오나요?", false));
        var claimNotice = sent(user.getId(), NotificationEventType.ORDER_CLAIM_RESOLVED, NotificationAggregateType.ORDER_CLAIM, claim.getId());
        var refundNotice = sent(user.getId(), NotificationEventType.DEPOSIT_REFUNDED, NotificationAggregateType.REFUND, refund.getId());
        var vacancyNotice = sent(user.getId(), NotificationEventType.BOOKING_VACANCY_AVAILABLE, NotificationAggregateType.VACANCY_ALERT, vacancy.getId());
        var qnaNotice = sent(user.getId(), NotificationEventType.PRODUCT_QNA_ANSWERED, NotificationAggregateType.PRODUCT_QNA, qna.getId());

        var results = query.listNotifications(user.getId(), 0, 20, false);
        assertThat(row(results, claimNotice)).satisfies(row -> {
            assertThat(row.contextTitle()).isEqualTo("주문 #" + order.getId() + " · 구매 당시 작품명");
            assertThat(row.target()).isEqualTo(new NotificationTarget(order.getId(), null, null, null, null, null));
        });
        assertThat(row(results, refundNotice)).satisfies(row -> {
            assertThat(row.contextTitle()).isEqualTo("예약 #" + booking.getId() + " · 가죽공예");
            assertThat(row.scheduledAt()).isEqualTo(bookedSlot.getStartAt().withNano(0));
            assertThat(row.target()).isEqualTo(new NotificationTarget(null, booking.getId(), null, null, null, null));
        });
        assertThat(row(results, vacancyNotice)).satisfies(row -> {
            assertThat(row.contextTitle()).isEqualTo("빈자리 · 가죽공예");
            assertThat(row.scheduledAt()).isEqualTo(fullSlot.getStartAt().withNano(0));
            assertThat(row.target()).isEqualTo(
                    new NotificationTarget(null, null, null, null, bookingClass.getId(), fullSlot.getId()));
        });
        assertThat(row(results, qnaNotice)).satisfies(row -> {
            assertThat(row.contextTitle()).isEqualTo("상품 문의 · 문의한 작품");
            assertThat(row.target()).isEqualTo(new NotificationTarget(null, null, product.getId(), null, null, null));
        });
    }

    @Test
    @DisplayName("알림 원본이 없거나 타인 소유이면 관련 내용은 비우고 본인 알림만 반환한다")
    void inbox_omitsMissingAndForeignAggregateDetails() {
        var now = LocalDateTime.now(clock).withNano(0);
        var owner = users.save(new User("notification-owner@example.com", "hash", "회원", "01098760002"));
        var other = users.save(new User("notification-other@example.com", "hash", "다른 회원", "01098760003"));
        var product = products.save(readyStockProduct("타인 작품", 10_000L));
        var order = orderStore.save(memberOrder(other.getId(), 10_000L, now, now.plusDays(1)));
        orderItems.save(readyStockOrderItem(order, product.getId(), "타인 구매 내용", 1, 10_000L));
        var foreign = sent(owner.getId(), NotificationEventType.ORDER_PAID, NotificationAggregateType.ORDER, order.getId());
        var missing = sent(owner.getId(), NotificationEventType.ORDER_PAID, NotificationAggregateType.ORDER, Long.MAX_VALUE);
        sent(other.getId(), NotificationEventType.ORDER_PAID, NotificationAggregateType.ORDER, order.getId());
        var results = query.listNotifications(owner.getId(), 0, 20, false);
        assertThat(results).extracting(NotificationView::id)
                .containsExactlyInAnyOrder(foreign, missing);
        assertThat(results).allSatisfy(row -> {
            assertThat(row.contextTitle()).isNull();
            assertThat(row.scheduledAt()).isNull();
            assertThat(row.target()).isEqualTo(NotificationTarget.NONE);
        });
    }

    private static NotificationView row(List<NotificationView> results, Long notificationId) {
        return results.stream().filter(row -> row.id().equals(notificationId)).findFirst().orElseThrow();
    }

    private Long sent(Long userId, NotificationEventType eventType, NotificationAggregateType aggregateType, Long aggregateId) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            var now = LocalDateTime.now(clock).withNano(0);
            var outbox = outboxRepository.save(NotificationOutbox.from(
                    NotificationRequestedEvent.forUser(userId, eventType, aggregateType, aggregateId), now));
            outbox.markSent(outbox.markProcessing(now), now);
            return outbox.getId();
        });
    }
}
