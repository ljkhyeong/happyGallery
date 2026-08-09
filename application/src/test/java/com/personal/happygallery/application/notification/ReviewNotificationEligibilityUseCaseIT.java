package com.personal.happygallery.application.notification;

import com.personal.happygallery.adapter.out.persistence.admin.AdminUserRepository;
import com.personal.happygallery.adapter.out.persistence.booking.BookingRepository;
import com.personal.happygallery.adapter.out.persistence.booking.ClassRepository;
import com.personal.happygallery.adapter.out.persistence.booking.SlotRepository;
import com.personal.happygallery.adapter.out.persistence.notification.NotificationOutboxRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderItemRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.review.ReviewRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.notification.port.out.NotificationReminderRecipient;
import com.personal.happygallery.application.notification.port.out.ReviewNotificationEligibilityPort;
import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.order.OrderItem;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewModerationAction;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class ReviewNotificationEligibilityUseCaseIT {

    @Autowired ReviewNotificationEligibilityPort eligibilityPort;
    @Autowired NotificationOutboxTransactionService outboxTransactionService;
    @Autowired NotificationOutboxRepository notificationOutboxRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository orderItemRepository;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ReviewModerationPort reviewModerationPort;
    @Autowired AdminUserRepository adminUserRepository;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearNotificationLogs();
        cleanupSupport.clearOrderData();
        cleanupSupport.clearBookingData();
        cleanupSupport.clearUsers();
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("주문 후기 요청은 미작성 또는 재작성 허용 원천만 현재 기회로 인정한다")
    @Test
    void findOrderRequestRecipient_checksLiveAndBlockedReviewTombstones() {
        User user = createUser("order-review-notice@example.com", "01071000001");
        Product product = productRepository.saveAndFlush(
                new Product("후기 알림 상품", ProductType.READY_STOCK, 30_000L));
        Order order = completedOrder(user, product.getPrice());
        OrderItem orderItem = orderItemRepository.saveAndFlush(
                new OrderItem(order, product.getId(), product.getName(), 1, product.getPrice()));
        NotificationReminderRecipient recipient = NotificationReminderRecipient.forUser(user.getId());

        assertThat(eligibilityPort.findOrderRequestRecipient(order.getId())).contains(recipient);

        Review review = reviewRepository.saveAndFlush(Review.forProduct(
                user.getId(), orderItem.getId(), product.getId(), 5, "완성도가 좋아요", now()));
        assertThat(eligibilityPort.findOrderRequestRecipient(order.getId())).isEmpty();

        softDelete(review.getId(), false);
        assertThat(eligibilityPort.findOrderRequestRecipient(order.getId())).contains(recipient);

        NotificationOutbox outbox = notificationOutboxRepository.saveAndFlush(NotificationOutbox.from(
                NotificationRequestedEvent.forUserOncePerAggregate(
                        user.getId(), NotificationEventType.REVIEW_REQUEST, "ORDER", order.getId()),
                now()));
        String processingToken = outbox.markProcessing(now());
        notificationOutboxRepository.saveAndFlush(outbox);

        Review replacement = reviewRepository.saveAndFlush(Review.forProduct(
                user.getId(), orderItem.getId(), product.getId(), 4, "다시 작성한 후기예요", now()));
        assertThat(eligibilityPort.findOrderRequestRecipient(order.getId())).isEmpty();
        assertThat(outboxTransactionService.prepareDelivery(outbox.getId(), processingToken).status())
                .isEqualTo(NotificationOutboxPreparationStatus.OBSOLETE);
        assertThat(notificationOutboxRepository.findById(outbox.getId()))
                .hasValueSatisfying(saved -> assertSoftly(softly -> {
                    softly.assertThat(saved.getStatus()).isEqualTo(NotificationOutboxStatus.OBSOLETE);
                    softly.assertThat(saved.getLastError()).isEqualTo("REVIEW_NO_LONGER_RELEVANT");
                }));

        softDelete(replacement.getId(), true);
        assertThat(eligibilityPort.findOrderRequestRecipient(order.getId())).isEmpty();
    }

    @DisplayName("예약 후기 요청은 완료 회원 예약의 현재 재작성 가능 여부를 확인한다")
    @Test
    void findBookingRequestRecipient_checksCompletedMemberBookingAndReview() {
        User user = createUser("booking-review-notice@example.com", "01071000002");
        BookingClass bookingClass = classRepository.saveAndFlush(
                new BookingClass("후기 알림 클래스", "CRAFT", 120, 50_000L, 30));
        Slot slot = slotRepository.saveAndFlush(
                new Slot(bookingClass, now().minusHours(3), now().minusHours(1)));
        Booking booking = Booking.forMemberDeposit(
                user, slot, 0L, 0L, DepositPaymentMethod.CARD);
        booking.complete(now());
        booking = bookingRepository.saveAndFlush(booking);
        NotificationReminderRecipient recipient = NotificationReminderRecipient.forUser(user.getId());

        assertThat(eligibilityPort.findBookingRequestRecipient(booking.getId())).contains(recipient);

        Review review = reviewRepository.saveAndFlush(Review.forClass(
                user.getId(), booking.getId(), bookingClass.getId(), 4, "즐겁게 배웠어요", now()));
        assertThat(eligibilityPort.findBookingRequestRecipient(booking.getId())).isEmpty();

        softDelete(review.getId(), false);
        assertThat(eligibilityPort.findBookingRequestRecipient(booking.getId())).contains(recipient);
    }

    @DisplayName("연속 숨김과 재공개 중 최신 조치와 현재 상태가 맞는 후기 알림만 허용한다")
    @Test
    void findReviewRecipients_checksCurrentStatusReplyAndDeletion() {
        User user = createUser("state-review-notice@example.com", "01071000003");
        Product product = productRepository.saveAndFlush(
                new Product("상태 알림 상품", ProductType.READY_STOCK, 40_000L));
        Order order = completedOrder(user, product.getPrice());
        OrderItem orderItem = orderItemRepository.saveAndFlush(
                new OrderItem(order, product.getId(), product.getName(), 1, product.getPrice()));
        Review review = reviewRepository.saveAndFlush(Review.forProduct(
                user.getId(), orderItem.getId(), product.getId(), 4, "정성스러운 작품이에요", now()));
        AdminUser admin = adminUserRepository.saveAndFlush(
                new AdminUser("review-notification-admin", "password-hash"));
        NotificationReminderRecipient recipient = NotificationReminderRecipient.forUser(user.getId());

        assertSoftly(softly -> {
            softly.assertThat(eligibilityPort.findRepublishedReviewRecipient(999_001L)).isEmpty();
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(999_002L)).isEmpty();
            softly.assertThat(eligibilityPort.findOwnerRepliedReviewRecipient(review.getId())).isEmpty();
        });

        ReviewModerationAction firstHide = reviewModerationPort.save(ReviewModerationAction.hide(
                review.getId(), "첫 번째 숨김", admin.getId(), now()));
        hide(review.getId(), admin.getId(), "첫 번째 숨김");

        assertSoftly(softly -> {
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(firstHide.getId()))
                    .contains(recipient);
            softly.assertThat(eligibilityPort.findOwnerRepliedReviewRecipient(review.getId())).isEmpty();
        });

        ReviewModerationAction republish = reviewModerationPort.save(ReviewModerationAction.republish(
                review.getId(), admin.getId(), now()));
        republish(review.getId());

        assertSoftly(softly -> {
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(firstHide.getId())).isEmpty();
            softly.assertThat(eligibilityPort.findRepublishedReviewRecipient(republish.getId()))
                    .contains(recipient);
        });

        ReviewModerationAction latestHide = reviewModerationPort.save(ReviewModerationAction.hide(
                review.getId(), "두 번째 숨김", admin.getId(), now()));
        hide(review.getId(), admin.getId(), "두 번째 숨김");
        addReply(review.getId(), admin.getId());

        assertSoftly(softly -> {
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(firstHide.getId())).isEmpty();
            softly.assertThat(eligibilityPort.findRepublishedReviewRecipient(republish.getId())).isEmpty();
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(latestHide.getId()))
                    .contains(recipient);
            softly.assertThat(eligibilityPort.findOwnerRepliedReviewRecipient(review.getId()))
                    .contains(recipient);
        });

        softDelete(review.getId(), true);
        assertSoftly(softly -> {
            softly.assertThat(eligibilityPort.findHiddenReviewRecipient(latestHide.getId())).isEmpty();
            softly.assertThat(eligibilityPort.findRepublishedReviewRecipient(republish.getId())).isEmpty();
            softly.assertThat(eligibilityPort.findOwnerRepliedReviewRecipient(review.getId())).isEmpty();
        });
    }

    private User createUser(String email, String phone) {
        return userStorePort.save(new User(email, "password-hash", "후기 알림 회원", phone));
    }

    private Order completedOrder(User user, long totalAmount) {
        Order order = Order.forMember(
                user.getId(), totalAmount, now().minusHours(1), now().plusHours(23));
        order.approve();
        order.markPickupReady();
        order.confirmPickup();
        return orderRepository.saveAndFlush(order);
    }

    private void softDelete(Long reviewId, boolean recreationBlocked) {
        jdbcTemplate.update("""
                        UPDATE reviews
                        SET rating = NULL,
                            content = NULL,
                            deleted_at = ?,
                            recreation_blocked = ?
                        WHERE id = ?
                        """,
                now(), recreationBlocked, reviewId);
    }

    private void hide(Long reviewId, Long adminId, String reason) {
        jdbcTemplate.update("""
                        UPDATE reviews
                        SET status = 'HIDDEN',
                            hidden_reason = ?,
                            hidden_at = ?,
                            hidden_by_admin_id = ?,
                            recreation_blocked = TRUE
                        WHERE id = ?
                        """,
                reason, now(), adminId, reviewId);
    }

    private void republish(Long reviewId) {
        jdbcTemplate.update("""
                        UPDATE reviews
                        SET status = 'PUBLISHED',
                            hidden_reason = NULL,
                            hidden_at = NULL,
                            hidden_by_admin_id = NULL
                        WHERE id = ?
                        """,
                reviewId);
    }

    private void addReply(Long reviewId, Long adminId) {
        jdbcTemplate.update("""
                        UPDATE reviews
                        SET reply_content = '공방 공식 답글입니다.',
                            reply_admin_id = ?,
                            reply_created_at = ?
                        WHERE id = ?
                        """,
                adminId, now(), reviewId);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
