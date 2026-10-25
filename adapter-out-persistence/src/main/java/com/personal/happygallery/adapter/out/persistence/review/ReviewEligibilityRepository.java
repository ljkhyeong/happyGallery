package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewOpportunity;
import com.personal.happygallery.application.review.port.out.ReviewEligibilityPort;
import com.personal.happygallery.application.review.port.out.ReviewSourceReservationView;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** 후기 작성 자격·기회 조회. 저장과 목록·상세 조회는 {@link ReviewRepository}가 맡는다. */
public interface ReviewEligibilityRepository
        extends Repository<Review, Long>, ReviewEligibilityPort {

    @Query("""
            SELECT oi.id AS orderItemId,
                   oi.productId AS productId,
                   o.status AS orderStatus
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.id = :orderItemId
              AND o.userId = :userId
            """)
    Optional<ProductReviewSourceProjection> findOwnedProductSourceRow(
            @Param("userId") Long userId, @Param("orderItemId") Long orderItemId);

    @Query("""
            SELECT b.id AS bookingId,
                   c.id AS bookingClassId,
                   b.status AS bookingStatus
            FROM Booking b
            JOIN b.bookingClass c
            WHERE b.id = :bookingId
              AND b.userId = :userId
            """)
    Optional<ClassReviewSourceProjection> findOwnedClassSourceRow(
            @Param("userId") Long userId, @Param("bookingId") Long bookingId);

    @Override
    @Query("""
            SELECT CASE WHEN COUNT(o.id) > 0 THEN true ELSE false END
            FROM Order o WHERE o.id = :orderId AND o.userId = :userId
            """)
    boolean existsOwnedOrder(@Param("userId") Long userId, @Param("orderId") Long orderId);

    @Override
    @Query("""
            SELECT CASE WHEN COUNT(b.id) > 0 THEN true ELSE false END
            FROM Booking b WHERE b.id = :bookingId AND b.userId = :userId
            """)
    boolean existsOwnedBooking(@Param("userId") Long userId, @Param("bookingId") Long bookingId);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewSourceReservationView(
                   CASE WHEN r.deletedAt IS NULL THEN true ELSE false END,
                   r.recreationBlocked)
            FROM Review r
            WHERE r.orderItemId = :orderItemId
              AND (r.deletedAt IS NULL OR r.recreationBlocked = true)
            """)
    @Override
    Optional<ReviewSourceReservationView> findProductSourceReservation(
            @Param("orderItemId") Long orderItemId);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewSourceReservationView(
                   CASE WHEN r.deletedAt IS NULL THEN true ELSE false END,
                   r.recreationBlocked)
            FROM Review r
            WHERE r.bookingId = :bookingId
              AND (r.deletedAt IS NULL OR r.recreationBlocked = true)
            """)
    @Override
    Optional<ReviewSourceReservationView> findClassSourceReservation(
            @Param("bookingId") Long bookingId);

    @Query("""
            SELECT oi.id AS sourceId,
                   oi.productId AS targetId,
                   oi.productName AS targetName,
                   o.id AS orderId,
                   COALESCE(MAX(h.decidedAt), o.createdAt) AS completedAt
            FROM OrderItem oi
            JOIN oi.order o
            LEFT JOIN com.personal.happygallery.domain.order.OrderApprovalHistory h
              ON h.orderId = o.id
             AND h.decision IN (
                  com.personal.happygallery.domain.order.OrderApprovalDecision.DELIVER,
                  com.personal.happygallery.domain.order.OrderApprovalDecision.PICKUP_COMPLETE)
            WHERE o.userId = :userId
              AND o.status IN (
                   com.personal.happygallery.domain.order.OrderStatus.DELIVERED,
                   com.personal.happygallery.domain.order.OrderStatus.PICKED_UP,
                   com.personal.happygallery.domain.order.OrderStatus.COMPLETED)
              AND NOT EXISTS (
                   SELECT r.id FROM Review r
                   WHERE r.orderItemId = oi.id
                     AND (r.deletedAt IS NULL OR r.recreationBlocked = true))
            GROUP BY oi.id, oi.productId, oi.productName, o.id, o.createdAt
            HAVING :cursorCompletedAt IS NULL
                OR COALESCE(MAX(h.decidedAt), o.createdAt) < :cursorCompletedAt
                OR (COALESCE(MAX(h.decidedAt), o.createdAt) = :cursorCompletedAt
                    AND :cursorTargetsProduct = true
                    AND oi.id < :cursorSourceId)
            ORDER BY COALESCE(MAX(h.decidedAt), o.createdAt) DESC, oi.id DESC
            """)
    List<ProductOpportunityProjection> findProductOpportunityRows(
            @Param("userId") Long userId,
            @Param("cursorCompletedAt") LocalDateTime cursorCompletedAt,
            @Param("cursorTargetsProduct") boolean cursorTargetsProduct,
            @Param("cursorSourceId") Long cursorSourceId,
            Pageable pageable);

    @Query("""
            SELECT b.id AS sourceId,
                   c.id AS targetId,
                   c.name AS targetName,
                   b.id AS bookingId,
                   COALESCE(MAX(h.createdAt), s.endAt) AS completedAt
            FROM Booking b
            JOIN b.bookingClass c
            JOIN b.slot s
            LEFT JOIN com.personal.happygallery.domain.booking.BookingHistory h
              ON h.booking = b
             AND h.action = com.personal.happygallery.domain.booking.BookingHistoryAction.COMPLETED
            WHERE b.userId = :userId
              AND b.status = com.personal.happygallery.domain.booking.BookingStatus.COMPLETED
              AND NOT EXISTS (
                   SELECT r.id FROM Review r
                   WHERE r.bookingId = b.id
                     AND (r.deletedAt IS NULL OR r.recreationBlocked = true))
            GROUP BY b.id, c.id, c.name, s.endAt
            HAVING :cursorCompletedAt IS NULL
                OR COALESCE(MAX(h.createdAt), s.endAt) < :cursorCompletedAt
                OR (COALESCE(MAX(h.createdAt), s.endAt) = :cursorCompletedAt
                    AND (
                        :cursorTargetsProduct = true
                        OR (:cursorTargetsProduct = false
                            AND b.id < :cursorSourceId)
                    ))
            ORDER BY COALESCE(MAX(h.createdAt), s.endAt) DESC, b.id DESC
            """)
    List<ClassOpportunityProjection> findClassOpportunityRows(
            @Param("userId") Long userId,
            @Param("cursorCompletedAt") LocalDateTime cursorCompletedAt,
            @Param("cursorTargetsProduct") boolean cursorTargetsProduct,
            @Param("cursorSourceId") Long cursorSourceId,
            Pageable pageable);

    @Override
    default Optional<ProductReviewSource> findOwnedProductSource(Long userId, Long orderItemId) {
        return findOwnedProductSourceRow(userId, orderItemId)
                .map(item -> new ProductReviewSource(
                        item.getOrderItemId(), item.getProductId(), item.getOrderStatus()));
    }

    @Override
    default Optional<ClassReviewSource> findOwnedClassSource(Long userId, Long bookingId) {
        return findOwnedClassSourceRow(userId, bookingId)
                .map(item -> new ClassReviewSource(
                        item.getBookingId(), item.getBookingClassId(), item.getBookingStatus()));
    }

    @Override
    default List<ReviewOpportunity> findReviewOpportunities(
            Long userId,
            LocalDateTime cursorCompletedAt,
            ReviewTargetType cursorTargetType,
            Long cursorSourceId,
            int limit) {
        boolean cursorTargetsProduct = cursorTargetType == ReviewTargetType.PRODUCT;
        Stream<ReviewOpportunity> products = findProductOpportunityRows(
                        userId,
                        cursorCompletedAt,
                        cursorTargetsProduct,
                        cursorSourceId,
                        PageRequest.ofSize(limit)).stream()
                .map(row -> new ReviewOpportunity(
                        ReviewTargetType.PRODUCT,
                        row.getSourceId(),
                        row.getTargetId(),
                        row.getTargetName(),
                        row.getOrderId(),
                        null,
                        row.getCompletedAt()));
        Stream<ReviewOpportunity> classes = findClassOpportunityRows(
                        userId,
                        cursorCompletedAt,
                        cursorTargetsProduct,
                        cursorSourceId,
                        PageRequest.ofSize(limit)).stream()
                .map(row -> new ReviewOpportunity(
                        ReviewTargetType.CLASS,
                        row.getSourceId(),
                        row.getTargetId(),
                        row.getTargetName(),
                        null,
                        row.getBookingId(),
                        row.getCompletedAt()));
        return Stream.concat(products, classes)
                .sorted(Comparator
                        .comparing(
                                ReviewOpportunity::completedAt,
                                Comparator.reverseOrder())
                        .thenComparingInt(row -> opportunityTargetOrder(row.targetType()))
                        .thenComparing(
                                ReviewOpportunity::sourceId,
                                Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private static int opportunityTargetOrder(ReviewTargetType targetType) {
        return targetType == ReviewTargetType.PRODUCT ? 0 : 1;
    }

    interface ProductReviewSourceProjection {
        Long getOrderItemId();
        Long getProductId();
        OrderStatus getOrderStatus();
    }

    interface ClassReviewSourceProjection {
        Long getBookingId();
        Long getBookingClassId();
        BookingStatus getBookingStatus();
    }

    interface ProductOpportunityProjection {
        Long getSourceId();
        Long getTargetId();
        String getTargetName();
        Long getOrderId();
        LocalDateTime getCompletedAt();
    }

    interface ClassOpportunityProjection {
        Long getSourceId();
        Long getTargetId();
        String getTargetName();
        Long getBookingId();
        LocalDateTime getCompletedAt();
    }
}
