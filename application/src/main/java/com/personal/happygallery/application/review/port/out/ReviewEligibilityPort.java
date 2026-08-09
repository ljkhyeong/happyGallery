package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.order.OrderStatus;
import java.util.Optional;
import java.util.List;

/** 회원 후기 원천의 소유권과 완료 상태를 조회한다. */
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

    List<ReviewOpportunityView> findReviewOpportunities(Long userId, int limit);
}
