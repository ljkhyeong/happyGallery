package com.personal.happygallery.domain.review;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.Objects;

/** 완료된 주문 품목 또는 예약을 근거로 작성하는 회원 후기. */
@Entity
@Table(
        name = "reviews",
        indexes = {
                @Index(
                        name = "idx_reviews_product_public",
                        columnList = "product_id,status,deleted_at,created_at,id"),
                @Index(
                        name = "idx_reviews_product_rating_public",
                        columnList = "product_id,status,deleted_at,rating,created_at,id"),
                @Index(
                        name = "idx_reviews_class_public",
                        columnList = "booking_class_id,status,deleted_at,created_at,id"),
                @Index(
                        name = "idx_reviews_class_rating_public",
                        columnList = "booking_class_id,status,deleted_at,rating,created_at,id"),
                @Index(
                        name = "idx_reviews_user_created",
                        columnList = "user_id,deleted_at,created_at,id"),
                @Index(
                        name = "idx_reviews_admin_status_created",
                        columnList = "deleted_at,status,created_at,id"),
                @Index(
                        name = "idx_reviews_created",
                        columnList = "deleted_at,created_at,id")
        }
)
public class Review {

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_HIDDEN_REASON_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "booking_class_id")
    private Long bookingClassId;

    @Column
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewStatus status;

    @Column(name = "hidden_reason", length = MAX_HIDDEN_REASON_LENGTH)
    private String hiddenReason;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Column(name = "hidden_by_admin_id")
    private Long hiddenByAdminId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "recreation_blocked", nullable = false)
    private boolean recreationBlocked;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    /** 작성자가 관리하는 본문·평점·사진 묶음의 낙관적 동시성 번호. */
    @Column(name = "content_revision", nullable = false)
    private long contentRevision;

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @Column(name = "reply_admin_id")
    private Long replyAdminId;

    @Column(name = "reply_created_at")
    private LocalDateTime replyCreatedAt;

    @Column(name = "reply_edited_at")
    private LocalDateTime replyEditedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Review() {}

    private Review(Long userId,
                   Long orderItemId,
                   Long productId,
                   Long bookingId,
                   Long bookingClassId,
                   int rating,
                   String content,
                   LocalDateTime now) {
        this.userId = requirePositiveId(userId, "회원 ID");
        requireExactlyOneSource(orderItemId, productId, bookingId, bookingClassId);
        this.orderItemId = orderItemId;
        this.productId = productId;
        this.bookingId = bookingId;
        this.bookingClassId = bookingClassId;
        applyContent(rating, content);
        this.status = ReviewStatus.PUBLISHED;
        this.recreationBlocked = false;
        this.contentRevision = 1L;
        this.createdAt = Objects.requireNonNull(now, "후기 작성 시각은 필수입니다.");
        this.updatedAt = now;
    }

    public static Review forProduct(Long userId,
                                    Long orderItemId,
                                    Long productId,
                                    int rating,
                                    String content,
                                    LocalDateTime now) {
        return new Review(userId, orderItemId, productId, null, null, rating, content, now);
    }

    public static Review forClass(Long userId,
                                  Long bookingId,
                                  Long bookingClassId,
                                  int rating,
                                  String content,
                                  LocalDateTime now) {
        return new Review(userId, null, null, bookingId, bookingClassId, rating, content, now);
    }

    public void update(int rating, String content, LocalDateTime updatedAt) {
        requireActive();
        LocalDateTime now = Objects.requireNonNull(updatedAt, "후기 수정 시각은 필수입니다.");
        applyContent(rating, content);
        this.editedAt = now;
        recordContentChange(now);
    }

    public void recordContentChange(LocalDateTime changedAt) {
        requireActive();
        this.contentRevision = Math.addExact(contentRevision, 1L);
        this.updatedAt = Objects.requireNonNull(changedAt, "후기 콘텐츠 변경 시각은 필수입니다.");
    }

    public void requireContentRevision(long expectedContentRevision) {
        requireActive();
        if (expectedContentRevision < 1L || contentRevision != expectedContentRevision) {
            throw new HappyGalleryException(ErrorCode.REVIEW_CONTENT_CHANGED);
        }
    }

    public void requireVersion(long expectedVersion) {
        requireActive();
        if (expectedVersion < 0L || version != expectedVersion) {
            throw new HappyGalleryException(
                    ErrorCode.CONFLICT,
                    "불러온 뒤 후기 운영 상태가 변경되었습니다. 최신 상태를 다시 확인해 주세요.");
        }
    }

    /** 실제 상태 전이일 때만 true를 반환한다. */
    public boolean changeStatus(ReviewStatus status,
                                String reason,
                                Long adminUserId,
                                LocalDateTime changedAt) {
        requireActive();
        if (status == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "후기 상태는 필수입니다.");
        }
        if (this.status == status) {
            return false;
        }
        LocalDateTime now = Objects.requireNonNull(changedAt, "후기 상태 변경 시각은 필수입니다.");
        if (status == ReviewStatus.HIDDEN) {
            String normalizedReason = requireHiddenReason(reason);
            Long hidingAdminId = requirePositiveId(adminUserId, "관리자 ID");
            this.hiddenReason = normalizedReason;
            this.hiddenAt = now;
            this.hiddenByAdminId = hidingAdminId;
            this.recreationBlocked = true;
        } else {
            this.hiddenReason = null;
            this.hiddenAt = null;
            this.hiddenByAdminId = null;
        }
        this.status = status;
        this.updatedAt = now;
        return true;
    }

    /** 개인정보성 본문을 지우되 숨김 이력에 따른 원천 예약 여부는 보존한다. */
    public void softDelete(LocalDateTime deletedAt) {
        requireActive();
        LocalDateTime now = Objects.requireNonNull(deletedAt, "후기 삭제 시각은 필수입니다.");
        this.rating = null;
        this.content = null;
        this.replyContent = null;
        this.replyAdminId = null;
        this.replyCreatedAt = null;
        this.replyEditedAt = null;
        this.hiddenReason = null;
        this.hiddenAt = null;
        this.hiddenByAdminId = null;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    /** @return 최초 답글 작성이면 true, 기존 답글 수정이면 false */
    public boolean upsertOfficialReply(String content, Long adminUserId, LocalDateTime repliedAt) {
        requireActive();
        LocalDateTime now = Objects.requireNonNull(repliedAt, "공식 답글 시각은 필수입니다.");
        String validated = ContentTextPolicy.requireBody(content, "공식 답글");
        Long validatedAdminId = requirePositiveId(adminUserId, "관리자 ID");
        boolean created = this.replyContent == null;
        this.replyContent = validated;
        this.replyAdminId = validatedAdminId;
        if (created) {
            this.replyCreatedAt = now;
            this.replyEditedAt = null;
        } else {
            this.replyEditedAt = now;
        }
        this.updatedAt = now;
        return created;
    }

    public void removeOfficialReply(LocalDateTime removedAt) {
        requireActive();
        if (replyContent == null) {
            return;
        }
        this.replyContent = null;
        this.replyAdminId = null;
        this.replyCreatedAt = null;
        this.replyEditedAt = null;
        this.updatedAt = Objects.requireNonNull(removedAt, "공식 답글 삭제 시각은 필수입니다.");
    }

    public void requirePublicInteraction(Long actorUserId) {
        requireActive();
        if (status != ReviewStatus.PUBLISHED) {
            throw new HappyGalleryException(ErrorCode.REVIEW_INTERACTION_NOT_ALLOWED);
        }
        if (Objects.equals(userId, actorUserId)) {
            throw new HappyGalleryException(ErrorCode.REVIEW_SELF_INTERACTION_NOT_ALLOWED);
        }
    }

    public void requireActive() {
        if (deletedAt != null) {
            throw new HappyGalleryException(ErrorCode.REVIEW_DELETED);
        }
    }

    private void applyContent(int rating, String content) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "후기 별점은 " + MIN_RATING + "점 이상 " + MAX_RATING + "점 이하여야 합니다.");
        }
        String validatedContent = ContentTextPolicy.requireBody(content, "후기 내용");
        this.rating = rating;
        this.content = validatedContent;
    }

    private static void requireExactlyOneSource(Long orderItemId,
                                                Long productId,
                                                Long bookingId,
                                                Long bookingClassId) {
        boolean productSource = orderItemId != null && productId != null
                && bookingId == null && bookingClassId == null;
        boolean classSource = orderItemId == null && productId == null
                && bookingId != null && bookingClassId != null;
        if (!productSource && !classSource) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "후기는 주문 품목 또는 예약 중 하나의 원천만 가져야 합니다.");
        }
        if (productSource) {
            requirePositiveId(orderItemId, "주문 품목 ID");
            requirePositiveId(productId, "상품 ID");
        } else {
            requirePositiveId(bookingId, "예약 ID");
            requirePositiveId(bookingClassId, "클래스 ID");
        }
    }

    private static Long requirePositiveId(Long value, String fieldName) {
        if (value == null || value < 1L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, fieldName + "가 올바르지 않습니다.");
        }
        return value;
    }

    private static String requireHiddenReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "후기 숨김 사유는 필수입니다.");
        }
        String normalized = reason.strip();
        if (normalized.length() > MAX_HIDDEN_REASON_LENGTH) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "후기 숨김 사유는 " + MAX_HIDDEN_REASON_LENGTH + "자 이하여야 합니다.");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getOrderItemId() { return orderItemId; }
    public Long getProductId() { return productId; }
    public Long getBookingId() { return bookingId; }
    public Long getBookingClassId() { return bookingClassId; }
    public Integer getRating() { return rating; }
    public String getContent() { return content; }
    public ReviewStatus getStatus() { return status; }
    public String getHiddenReason() { return hiddenReason; }
    public LocalDateTime getHiddenAt() { return hiddenAt; }
    public Long getHiddenByAdminId() { return hiddenByAdminId; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public boolean isRecreationBlocked() { return recreationBlocked; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public long getContentRevision() { return contentRevision; }
    public String getReplyContent() { return replyContent; }
    public Long getReplyAdminId() { return replyAdminId; }
    public LocalDateTime getReplyCreatedAt() { return replyCreatedAt; }
    public LocalDateTime getReplyEditedAt() { return replyEditedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public ReviewTargetType getTargetType() {
        return productId != null ? ReviewTargetType.PRODUCT : ReviewTargetType.CLASS;
    }

    public Long getSourceId() {
        return orderItemId != null ? orderItemId : bookingId;
    }

    public Long getTargetId() {
        return productId != null ? productId : bookingClassId;
    }

    public boolean isDeleted() { return deletedAt != null; }
}
