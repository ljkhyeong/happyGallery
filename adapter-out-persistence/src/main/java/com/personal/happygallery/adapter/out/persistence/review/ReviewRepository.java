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

    String VIEW_CONSTRUCTOR_PREFIX = """
            SELECT new com.personal.happygallery.application.review.port.out.ReviewListView(
                   r.id,
                   r.userId,
                   r.orderItemId,
                   r.productId,
                   r.bookingId,
                   r.bookingClassId,
            """;

    String VIEW_CONSTRUCTOR_SUFFIX = """
                   r.rating,
                   r.content,
                   r.status,
                   r.contentRevision,
                   r.version,
                   r.hiddenReason,
                   r.hiddenAt,
                   r.hiddenByAdminId,
                   r.createdAt,
                   r.updatedAt,
                   r.editedAt,
                   r.replyContent,
                   r.replyAdminId,
                   r.replyCreatedAt,
                   r.replyEditedAt)
            """;

    String BASE_VIEW = VIEW_CONSTRUCTOR_PREFIX + """
                   CASE WHEN p.id IS NOT NULL THEN p.name ELSE c.name END,
            """ + VIEW_CONSTRUCTOR_SUFFIX + """
            FROM Review r
            LEFT JOIN Product p ON p.id = r.productId
            LEFT JOIN BookingClass c ON c.id = r.bookingClassId
            """;

    String PRODUCT_PUBLIC_VIEW = VIEW_CONSTRUCTOR_PREFIX + """
                   p.name,
            """ + VIEW_CONSTRUCTOR_SUFFIX + """
            FROM Review r
            JOIN Product p ON p.id = r.productId
            """;

    String CLASS_PUBLIC_VIEW = VIEW_CONSTRUCTOR_PREFIX + """
                   c.name,
            """ + VIEW_CONSTRUCTOR_SUFFIX + """
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

    @Override
    @Query(BASE_VIEW + " WHERE r.id = :reviewId AND r.deletedAt IS NULL")
    Optional<ReviewListView> findViewById(@Param("reviewId") Long reviewId);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewInteractionStateView(
                   r.id, r.userId, r.status)
            FROM Review r
            WHERE r.id IN :reviewIds
              AND r.deletedAt IS NULL
            """)
    List<ReviewInteractionStateView> findInteractionStateRows(
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
    List<ReviewListView> findPublishedProductLatestRows(
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
    List<ReviewListView> findPublishedProductRatingHighRows(
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
    List<ReviewListView> findPublishedProductRatingLowRows(
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
    List<ReviewListView> findPublishedClassLatestRows(
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
    List<ReviewListView> findPublishedClassRatingHighRows(
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
    List<ReviewListView> findPublishedClassRatingLowRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewSummaryView(
                   COUNT(r.id),
                   COALESCE(AVG(r.rating), 0.0),
                   COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0))
            FROM Review r
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
            """)
    @Override
    ReviewSummaryView summarizePublishedProduct(@Param("productId") Long productId);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewSummaryView(
                   COUNT(r.id),
                   COALESCE(AVG(r.rating), 0.0),
                   COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0))
            FROM Review r
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
            """)
    @Override
    ReviewSummaryView summarizePublishedClass(@Param("classId") Long classId);

    @Override
    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.productId = :productId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    long countPublishedProduct(
            @Param("productId") Long productId, @Param("rating") Integer rating);

    @Override
    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.bookingClassId = :classId
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
              AND (:rating IS NULL OR r.rating = :rating)
            """)
    long countPublishedClass(
            @Param("classId") Long classId, @Param("rating") Integer rating);

    @Query(BASE_VIEW + """
            WHERE r.userId = :userId
              AND r.deletedAt IS NULL
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewListView> findUserRows(@Param("userId") Long userId, Pageable pageable);

    @Query(BASE_VIEW + """
            WHERE r.userId = :userId
              AND r.deletedAt IS NULL
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewListView> findUserRowsAfter(
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
    @Override
    List<ReviewListView> findByOwnedOrder(
            @Param("userId") Long userId, @Param("orderId") Long orderId);

    @Query(BASE_VIEW + """
            JOIN Booking b ON b.id = r.bookingId
            WHERE b.id = :bookingId
              AND b.userId = :userId
              AND r.userId = :userId
              AND r.deletedAt IS NULL
            ORDER BY r.id ASC
            """)
    @Override
    List<ReviewListView> findByOwnedBooking(
            @Param("userId") Long userId, @Param("bookingId") Long bookingId);

    @Query(BASE_VIEW + """
            WHERE r.deletedAt IS NULL
              AND (:status IS NULL OR r.status = :status)
              AND (:productOnly = false OR r.productId IS NOT NULL)
              AND (:classOnly = false OR r.bookingClassId IS NOT NULL)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewListView> findAdminRows(
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
    List<ReviewListView> findAdminRowsAfter(
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
    default List<ReviewInteractionStateView> findInteractionStates(List<Long> reviewIds) {
        if (reviewIds.isEmpty()) {
            return List.of();
        }
        return findInteractionStateRows(reviewIds);
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
        return switch (sort) {
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
        return switch (sort) {
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
    }

    @Override
    default List<ReviewListView> findByUserId(Long userId, int limit) {
        return findUserRows(userId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ReviewListView> findByUserIdAfter(
            Long userId, LocalDateTime createdAt, Long id, int limit) {
        return findUserRowsAfter(userId, createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    default List<ReviewListView> findForAdmin(
            ReviewTargetType targetType, ReviewStatus status, int limit) {
        return findAdminRows(
                status,
                targetType == ReviewTargetType.PRODUCT,
                targetType == ReviewTargetType.CLASS,
                PageRequest.ofSize(limit));
    }

    @Override
    default List<ReviewListView> findForAdminAfter(
            ReviewTargetType targetType,
            ReviewStatus status,
            LocalDateTime createdAt,
            Long id,
            int limit) {
        return findAdminRowsAfter(
                status,
                targetType == ReviewTargetType.PRODUCT,
                targetType == ReviewTargetType.CLASS,
                createdAt,
                id,
                PageRequest.ofSize(limit));
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
    default List<ReviewOpportunityView> findReviewOpportunities(
            Long userId,
            LocalDateTime cursorCompletedAt,
            ReviewTargetType cursorTargetType,
            Long cursorSourceId,
            int limit) {
        boolean cursorTargetsProduct = cursorTargetType == ReviewTargetType.PRODUCT;
        Stream<ReviewOpportunityView> products = findProductOpportunityRows(
                        userId,
                        cursorCompletedAt,
                        cursorTargetsProduct,
                        cursorSourceId,
                        PageRequest.ofSize(limit)).stream()
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
                        PageRequest.ofSize(limit)).stream()
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
