package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 후기 대상 주문 품목·예약이 회원 본인 소유인지, 완료됐는지 조회한다. */
public interface ReviewEligibilityPort {

    record ProductReviewSource(
            Long orderItemId,
            Long productId,
            OrderStatus orderStatus
    ) {}

    record ClassReviewSource(
            Long bookingId,
            Long bookingClassId,
            BookingStatus bookingStatus
    ) {}

    Optional<ProductReviewSource> findOwnedProductSource(Long userId, Long orderItemId);

    Optional<ClassReviewSource> findOwnedClassSource(Long userId, Long bookingId);

    boolean existsOwnedOrder(Long userId, Long orderId);

    boolean existsOwnedBooking(Long userId, Long bookingId);

    Optional<ReviewSourceReservationView> findProductSourceReservation(Long orderItemId);

    Optional<ReviewSourceReservationView> findClassSourceReservation(Long bookingId);

    List<ReviewOpportunityView> findReviewOpportunities(
            Long userId,
            LocalDateTime cursorCompletedAt,
            ReviewTargetType cursorTargetType,
            Long cursorSourceId,
            int limit);
}
