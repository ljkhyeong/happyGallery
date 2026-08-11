package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewEligibilityPort;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewInteractionStateView;
import com.personal.happygallery.application.review.port.out.ReviewOpportunityView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewSourceReservationView;
import com.personal.happygallery.application.review.port.out.ReviewSummaryView;
import com.personal.happygallery.domain.booking.BookingStatus;
import com.personal.happygallery.domain.order.OrderStatus;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface ReviewRepository
        extends JpaRepository<Review, Long>, ReviewReaderPort, ReviewEligibilityPort {

    String VIEW_COLUMNS = """
            SELECT r.id AS id,
                   r.userId AS userId,
                   r.orderItemId AS orderItemId,
                   r.productId AS productId,
                   r.bookingId AS bookingId,
                   r.bookingClassId AS bookingClassId,
                   r.rating AS rating,
                   r.content AS content,
                   r.status AS status,
                   r.contentRevision AS contentRevision,
                   r.version AS version,
                   r.hiddenReason AS hiddenReason,
                   r.hiddenAt AS hiddenAt,
                   r.hiddenByAdminId AS hiddenByAdminId,
                   r.createdAt AS createdAt,
                   r.updatedAt AS updatedAt,
                   r.editedAt AS editedAt,
                   r.replyContent AS replyContent,
                   r.replyAdminId AS replyAdminId,
                   r.replyCreatedAt AS replyCreatedAt,
                   r.replyEditedAt AS replyEditedAt
            """;

    String BASE_VIEW = VIEW_COLUMNS + """
                   , CASE WHEN p.id IS NOT NULL THEN p.name ELSE c.name END AS targetName
            FROM Review r
            LEFT JOIN Product p ON p.id = r.productId
            LEFT JOIN BookingClass c ON c.id = r.bookingClassId
            """;

    String PRODUCT_PUBLIC_VIEW = VIEW_COLUMNS + """
                   , p.name AS targetName
            FROM Review r
            JOIN Product p ON p.id = r.productId
            """;

    String CLASS_PUBLIC_VIEW = VIEW_COLUMNS + """
                   , c.name AS targetName
            FROM Review r
            JOIN BookingClass c ON c.id = r.bookingClassId
            """;

    @Override
    @Query("""
            SELECT r FROM Review r
            WHERE r.id = :reviewId
              AND r.userId = :userId
              AND r.deletedAt IS NULL
            """)
    Optional<Review> findByIdAndUserId(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Review r
            WHERE r.id = :reviewId
              AND r.userId = :userId
              AND r.deletedAt IS NULL
            """)
    Optional<Review> findByIdAndUserIdForUpdate(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :reviewId AND r.deletedAt IS NULL")
    Optional<Review> findByIdForUpdate(@Param("reviewId") Long reviewId);

    @Query(BASE_VIEW + " WHERE r.id = :reviewId AND r.deletedAt IS NULL")
    Optional<ReviewRowProjection> findViewRowById(@Param("reviewId") Long reviewId);

    @Query("""
            SELECT r.id AS reviewId, r.userId AS ownerUserId, r.status AS status
            FROM Review r
            WHERE r.id IN :reviewIds
              AND r.deletedAt IS NULL
            """)
    List<ReviewInteractionStateProjection> findInteractionStateRows(
            @Param("reviewIds") List<Long> reviewIds);

    @Query(PRODUCT_PUBLIC_VIEW + """
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.createdAt < :cursorCreatedAt
                    OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)
              )
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedProductLatestRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(PRODUCT_PUBLIC_VIEW + """
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.rating < :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating DESC, r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedProductRatingHighRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(PRODUCT_PUBLIC_VIEW + """
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.rating > :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating ASC, r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedProductRatingLowRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + """
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.createdAt < :cursorCreatedAt
                    OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)
              )
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedClassLatestRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + """
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.rating < :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating DESC, r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedClassRatingHighRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + """
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
              AND (
                    :cursorId IS NULL
                    OR r.rating > :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating ASC, r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findPublishedClassRatingLowRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT COUNT(r.id) AS reviewCount,
                   COALESCE(AVG(r.rating), 0.0) AS averageRating,
                   COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0) AS rating1,
                   COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0) AS rating2,
                   COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0) AS rating3,
                   COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0) AS rating4,
                   COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0) AS rating5
            FROM Review r
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
            """)
    ReviewSummaryProjection summarizeProductRow(@Param("productId") Long productId);

    @Query("""
            SELECT COUNT(r.id) AS reviewCount,
                   COALESCE(AVG(r.rating), 0.0) AS averageRating,
                   COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0) AS rating1,
                   COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0) AS rating2,
                   COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0) AS rating3,
                   COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0) AS rating4,
                   COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0) AS rating5
            FROM Review r
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
            """)
    ReviewSummaryProjection summarizeClassRow(@Param("classId") Long classId);

    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    long countPublishedProductRows(
            @Param("productId") Long productId, @Param("rating") Integer rating);

    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    long countPublishedClassRows(
            @Param("classId") Long classId, @Param("rating") Integer rating);

    @Query(BASE_VIEW + """
            WHERE r.userId = :userId
              AND r.deletedAt IS NULL
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findUserRows(@Param("userId") Long userId, Pageable pageable);

    @Query(BASE_VIEW + """
            WHERE r.userId = :userId
              AND r.deletedAt IS NULL
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findUserRowsAfter(
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    @Query(BASE_VIEW + """
            JOIN OrderItem oi ON oi.id = r.orderItemId
            JOIN oi.order o
            WHERE o.id = :orderId
              AND o.userId = :userId
              AND r.userId = :userId
              AND r.deletedAt IS NULL
            ORDER BY oi.id ASC, r.id ASC
            """)
    List<ReviewRowProjection> findOwnedOrderRows(
            @Param("userId") Long userId, @Param("orderId") Long orderId);

    @Query(BASE_VIEW + """
            JOIN Booking b ON b.id = r.bookingId
            WHERE b.id = :bookingId
              AND b.userId = :userId
              AND r.userId = :userId
              AND r.deletedAt IS NULL
            ORDER BY r.id ASC
            """)
    List<ReviewRowProjection> findOwnedBookingRows(
            @Param("userId") Long userId, @Param("bookingId") Long bookingId);

    @Query(BASE_VIEW + """
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:productOnly = false OR r.productId IS NOT NULL)
              AND (:classOnly = false OR r.bookingClassId IS NOT NULL)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findAdminRows(
            @Param("status") ReviewStatus status,
            @Param("productOnly") boolean productOnly,
            @Param("classOnly") boolean classOnly,
            Pageable pageable);

    @Query(BASE_VIEW + """
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:productOnly = false OR r.productId IS NOT NULL)
              AND (:classOnly = false OR r.bookingClassId IS NOT NULL)
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewRowProjection> findAdminRowsAfter(
            @Param("status") ReviewStatus status,
            @Param("productOnly") boolean productOnly,
            @Param("classOnly") boolean classOnly,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

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

    @Query("""
            SELECT CASE WHEN COUNT(o.id) > 0 THEN true ELSE false END
            FROM Order o WHERE o.id = :orderId AND o.userId = :userId
            """)
    boolean existsOwnedOrderRow(@Param("userId") Long userId, @Param("orderId") Long orderId);

    @Query("""
            SELECT CASE WHEN COUNT(b.id) > 0 THEN true ELSE false END
            FROM Booking b WHERE b.id = :bookingId AND b.userId = :userId
            """)
    boolean existsOwnedBookingRow(@Param("userId") Long userId, @Param("bookingId") Long bookingId);

    @Query("""
            SELECT CASE WHEN r.deletedAt IS NULL THEN true ELSE false END AS active,
                   r.recreationBlocked AS recreationBlocked
            FROM Review r
            WHERE r.orderItemId = :orderItemId
              AND (r.deletedAt IS NULL OR r.recreationBlocked = true)
            """)
    Optional<SourceReservationProjection> findProductSourceReservationRow(
            @Param("orderItemId") Long orderItemId);

    @Query("""
            SELECT CASE WHEN r.deletedAt IS NULL THEN true ELSE false END AS active,
                   r.recreationBlocked AS recreationBlocked
            FROM Review r
            WHERE r.bookingId = :bookingId
              AND (r.deletedAt IS NULL OR r.recreationBlocked = true)
            """)
    Optional<SourceReservationProjection> findClassSourceReservationRow(
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
    default Optional<ReviewListView> findViewById(Long reviewId) {
        return findViewRowById(reviewId).map(ReviewRepository::toView);
    }

    @Override
    default List<ReviewInteractionStateView> findInteractionStates(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return List.of();
        }
        return findInteractionStateRows(reviewIds).stream()
                .map(row -> new ReviewInteractionStateView(
                        row.getReviewId(), row.getOwnerUserId(), row.getStatus()))
                .toList();
    }

    @Override
    default List<ReviewListView> findPublishedByProduct(
            Long productId,
            Integer rating,
            ReviewSort sort,
            Integer cursorRating,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit) {
        Pageable pageable = PageRequest.ofSize(limit);
        List<ReviewRowProjection> rows = switch (sort) {
            case LATEST -> findPublishedProductLatestRows(
                    productId, rating, cursorCreatedAt, cursorId, pageable);
            case RATING_HIGH -> findPublishedProductRatingHighRows(
                    productId,
                    rating,
                    cursorRating,
                    cursorCreatedAt,
                    cursorId,
                    pageable);
            case RATING_LOW -> findPublishedProductRatingLowRows(
                    productId,
                    rating,
                    cursorRating,
                    cursorCreatedAt,
                    cursorId,
                    pageable);
        };
        return toViews(rows);
    }

    @Override
    default List<ReviewListView> findPublishedByClass(
            Long classId,
            Integer rating,
            ReviewSort sort,
            Integer cursorRating,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit) {
        Pageable pageable = PageRequest.ofSize(limit);
        List<ReviewRowProjection> rows = switch (sort) {
            case LATEST -> findPublishedClassLatestRows(
                    classId, rating, cursorCreatedAt, cursorId, pageable);
            case RATING_HIGH -> findPublishedClassRatingHighRows(
                    classId,
                    rating,
                    cursorRating,
                    cursorCreatedAt,
                    cursorId,
                    pageable);
            case RATING_LOW -> findPublishedClassRatingLowRows(
                    classId,
                    rating,
                    cursorRating,
                    cursorCreatedAt,
                    cursorId,
                    pageable);
        };
        return toViews(rows);
    }

    @Override
    default ReviewSummaryView summarizePublishedProduct(Long productId) {
        return toSummary(summarizeProductRow(productId));
    }

    @Override
    default ReviewSummaryView summarizePublishedClass(Long classId) {
        return toSummary(summarizeClassRow(classId));
    }

    @Override
    default long countPublishedProduct(Long productId, Integer rating) {
        return countPublishedProductRows(productId, rating);
    }

    @Override
    default long countPublishedClass(Long classId, Integer rating) {
        return countPublishedClassRows(classId, rating);
    }

    @Override
    default List<ReviewListView> findByUserId(Long userId, int limit) {
        return toViews(findUserRows(userId, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ReviewListView> findByUserIdAfter(
            Long userId, LocalDateTime createdAt, Long id, int limit) {
        return toViews(findUserRowsAfter(userId, createdAt, id, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ReviewListView> findByOwnedOrder(Long userId, Long orderId) {
        return toViews(findOwnedOrderRows(userId, orderId));
    }

    @Override
    default List<ReviewListView> findByOwnedBooking(Long userId, Long bookingId) {
        return toViews(findOwnedBookingRows(userId, bookingId));
    }

    @Override
    default List<ReviewListView> findForAdmin(
            ReviewTargetType targetType, ReviewStatus status, int limit) {
        return toViews(findAdminRows(
                status,
                targetType == ReviewTargetType.PRODUCT,
                targetType == ReviewTargetType.CLASS,
                PageRequest.ofSize(limit)));
    }

    @Override
    default List<ReviewListView> findForAdminAfter(
            ReviewTargetType targetType,
            ReviewStatus status,
            LocalDateTime createdAt,
            Long id,
            int limit) {
        return toViews(findAdminRowsAfter(
                status,
                targetType == ReviewTargetType.PRODUCT,
                targetType == ReviewTargetType.CLASS,
                createdAt,
                id,
                PageRequest.ofSize(limit)));
    }

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
    default boolean existsOwnedOrder(Long userId, Long orderId) {
        return existsOwnedOrderRow(userId, orderId);
    }

    @Override
    default boolean existsOwnedBooking(Long userId, Long bookingId) {
        return existsOwnedBookingRow(userId, bookingId);
    }

    @Override
    default Optional<ReviewSourceReservationView> findProductSourceReservation(Long orderItemId) {
        return findProductSourceReservationRow(orderItemId)
                .map(row -> new ReviewSourceReservationView(
                        row.getActive(), row.getRecreationBlocked()));
    }

    @Override
    default Optional<ReviewSourceReservationView> findClassSourceReservation(Long bookingId) {
        return findClassSourceReservationRow(bookingId)
                .map(row -> new ReviewSourceReservationView(
                        row.getActive(), row.getRecreationBlocked()));
    }

    @Override
    default List<ReviewOpportunityView> findReviewOpportunities(
            Long userId,
            LocalDateTime cursorCompletedAt,
            ReviewTargetType cursorTargetType,
            Long cursorSourceId,
            int limit) {
        int sourceLimit = Math.max(1, limit);
        boolean cursorTargetsProduct = cursorTargetType == ReviewTargetType.PRODUCT;
        Stream<ReviewOpportunityView> products = findProductOpportunityRows(
                        userId,
                        cursorCompletedAt,
                        cursorTargetsProduct,
                        cursorSourceId,
                        PageRequest.ofSize(sourceLimit)).stream()
                .map(row -> new ReviewOpportunityView(
                        ReviewTargetType.PRODUCT,
                        row.getSourceId(),
                        row.getTargetId(),
                        row.getTargetName(),
                        row.getOrderId(),
                        null,
                        row.getCompletedAt()));
        Stream<ReviewOpportunityView> classes = findClassOpportunityRows(
                        userId,
                        cursorCompletedAt,
                        cursorTargetsProduct,
                        cursorSourceId,
                        PageRequest.ofSize(sourceLimit)).stream()
                .map(row -> new ReviewOpportunityView(
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
                                ReviewOpportunityView::completedAt,
                                Comparator.reverseOrder())
                        .thenComparingInt(row -> opportunityTargetOrder(row.targetType()))
                        .thenComparing(
                                ReviewOpportunityView::sourceId,
                                Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private static int opportunityTargetOrder(ReviewTargetType targetType) {
        return targetType == ReviewTargetType.PRODUCT ? 0 : 1;
    }

    private static List<ReviewListView> toViews(List<ReviewRowProjection> rows) {
        return rows.stream().map(ReviewRepository::toView).toList();
    }

    private static ReviewListView toView(ReviewRowProjection row) {
        return new ReviewListView(
                row.getId(),
                row.getUserId(),
                row.getOrderItemId(),
                row.getProductId(),
                row.getBookingId(),
                row.getBookingClassId(),
                row.getTargetName(),
                row.getRating(),
                row.getContent(),
                row.getStatus(),
                row.getContentRevision(),
                row.getVersion(),
                row.getHiddenReason(),
                row.getHiddenAt(),
                row.getHiddenByAdminId(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getEditedAt(),
                row.getReplyContent(),
                row.getReplyAdminId(),
                row.getReplyCreatedAt(),
                row.getReplyEditedAt());
    }

    private static ReviewSummaryView toSummary(ReviewSummaryProjection row) {
        return new ReviewSummaryView(
                row.getReviewCount(),
                row.getAverageRating(),
                row.getRating1(),
                row.getRating2(),
                row.getRating3(),
                row.getRating4(),
                row.getRating5());
    }

    interface ReviewRowProjection {
        Long getId();
        Long getUserId();
        Long getOrderItemId();
        Long getProductId();
        Long getBookingId();
        Long getBookingClassId();
        String getTargetName();
        int getRating();
        String getContent();
        ReviewStatus getStatus();
        long getContentRevision();
        long getVersion();
        String getHiddenReason();
        LocalDateTime getHiddenAt();
        Long getHiddenByAdminId();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
        LocalDateTime getEditedAt();
        String getReplyContent();
        Long getReplyAdminId();
        LocalDateTime getReplyCreatedAt();
        LocalDateTime getReplyEditedAt();
    }

    interface ReviewInteractionStateProjection {
        Long getReviewId();
        Long getOwnerUserId();
        ReviewStatus getStatus();
    }

    interface ReviewSummaryProjection {
        long getReviewCount();
        double getAverageRating();
        long getRating1();
        long getRating2();
        long getRating3();
        long getRating4();
        long getRating5();
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

    interface SourceReservationProjection {
        boolean getActive();
        boolean getRecreationBlocked();
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
