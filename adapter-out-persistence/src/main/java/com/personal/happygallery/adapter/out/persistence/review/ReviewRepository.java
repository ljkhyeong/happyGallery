package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewInteractionStateView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewSummaryView;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

/** 후기 저장과 목록·상세 조회. 작성 자격·기회 조회는 {@link ReviewEligibilityRepository}가 맡는다. */
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewReaderPort {

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

    /** 삭제되지 않은 공개 후기만 남긴다. WHERE 절의 첫 조건 뒤에 붙인다. */
    String PUBLISHED_FILTER = """
              AND r.deletedAt IS NULL
              AND r.status = com.personal.happygallery.domain.review.ReviewStatus.PUBLISHED
            """;

    /** 공개 후기 중 선택한 별점만 남긴다. rating 파라미터를 사용한다. */
    String PUBLISHED_RATING_FILTER = PUBLISHED_FILTER + """
              AND (:rating IS NULL OR r.rating = :rating)
            """;

    /** 작품의 공개 후기 목록. productId·rating 파라미터를 사용하고 정렬별 커서 조각만 덧붙인다. */
    String PRODUCT_PUBLIC_VIEW = VIEW_CONSTRUCTOR_PREFIX + """
                   p.name,
            """ + VIEW_CONSTRUCTOR_SUFFIX + """
            FROM Review r
            JOIN Product p ON p.id = r.productId
            WHERE r.productId = :productId
            """ + PUBLISHED_RATING_FILTER;

    /** 클래스의 공개 후기 목록. classId·rating 파라미터를 사용하고 정렬별 커서 조각만 덧붙인다. */
    String CLASS_PUBLIC_VIEW = VIEW_CONSTRUCTOR_PREFIX + """
                   c.name,
            """ + VIEW_CONSTRUCTOR_SUFFIX + """
            FROM Review r
            JOIN BookingClass c ON c.id = r.bookingClassId
            WHERE r.bookingClassId = :classId
            """ + PUBLISHED_RATING_FILTER;

    /** 최신순 커서 조건과 정렬. cursorCreatedAt·cursorId 파라미터를 사용한다. */
    String LATEST_PAGE = """
              AND (
                    :cursorId IS NULL
                    OR r.createdAt < :cursorCreatedAt
                    OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)
              )
            ORDER BY r.createdAt DESC, r.id DESC
            """;

    /** 별점 높은순 커서 조건과 정렬. cursorRating·cursorCreatedAt·cursorId 파라미터를 사용한다. */
    String RATING_HIGH_PAGE = """
              AND (
                    :cursorId IS NULL
                    OR r.rating < :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating DESC, r.createdAt DESC, r.id DESC
            """;

    /** 별점 낮은순 커서 조건과 정렬. cursorRating·cursorCreatedAt·cursorId 파라미터를 사용한다. */
    String RATING_LOW_PAGE = """
              AND (
                    :cursorId IS NULL
                    OR r.rating > :cursorRating
                    OR (r.rating = :cursorRating
                        AND (r.createdAt < :cursorCreatedAt
                             OR (r.createdAt = :cursorCreatedAt AND r.id < :cursorId)))
              )
            ORDER BY r.rating ASC, r.createdAt DESC, r.id DESC
            """;

    String SUMMARY_VIEW = """
            SELECT new com.personal.happygallery.application.review.port.out.ReviewSummaryView(
                   COUNT(r.id),
                   COALESCE(AVG(r.rating), 0.0),
                   COALESCE(SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), 0))
            FROM Review r
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

    @Query(PRODUCT_PUBLIC_VIEW + LATEST_PAGE)
    List<ReviewListView> findPublishedProductLatestRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(PRODUCT_PUBLIC_VIEW + RATING_HIGH_PAGE)
    List<ReviewListView> findPublishedProductRatingHighRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(PRODUCT_PUBLIC_VIEW + RATING_LOW_PAGE)
    List<ReviewListView> findPublishedProductRatingLowRows(
            @Param("productId") Long productId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + LATEST_PAGE)
    List<ReviewListView> findPublishedClassLatestRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + RATING_HIGH_PAGE)
    List<ReviewListView> findPublishedClassRatingHighRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(CLASS_PUBLIC_VIEW + RATING_LOW_PAGE)
    List<ReviewListView> findPublishedClassRatingLowRows(
            @Param("classId") Long classId,
            @Param("rating") Integer rating,
            @Param("cursorRating") Integer cursorRating,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(SUMMARY_VIEW + """
            WHERE r.productId = :productId
            """ + PUBLISHED_FILTER)
    @Override
    ReviewSummaryView summarizePublishedProduct(@Param("productId") Long productId);

    @Query(SUMMARY_VIEW + """
            WHERE r.bookingClassId = :classId
            """ + PUBLISHED_FILTER)
    @Override
    ReviewSummaryView summarizePublishedClass(@Param("classId") Long classId);

    @Override
    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.productId = :productId
            """ + PUBLISHED_RATING_FILTER)
    long countPublishedProduct(
            @Param("productId") Long productId, @Param("rating") Integer rating);

    @Override
    @Query("""
            SELECT COUNT(r.id) FROM Review r
            WHERE r.bookingClassId = :classId
            """ + PUBLISHED_RATING_FILTER)
    long countPublishedClass(
            @Param("classId") Long classId, @Param("rating") Integer rating);

    @Query(BASE_VIEW + """
            WHERE r.userId = :userId
              AND r.deletedAt IS NULL
            """ + LATEST_PAGE)
    List<ReviewListView> findUserRows(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
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
            """ + LATEST_PAGE)
    List<ReviewListView> findAdminRows(
            @Param("status") ReviewStatus status,
            @Param("productOnly") boolean productOnly,
            @Param("classOnly") boolean classOnly,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
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
    default List<ReviewListView> findByUserId(
            Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findUserRows(userId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ReviewListView> findForAdmin(
            ReviewTargetType targetType,
            ReviewStatus status,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit) {
        return findAdminRows(
                status,
                targetType == ReviewTargetType.PRODUCT,
                targetType == ReviewTargetType.CLASS,
                cursorCreatedAt,
                cursorId,
                PageRequest.ofSize(limit));
    }
}
