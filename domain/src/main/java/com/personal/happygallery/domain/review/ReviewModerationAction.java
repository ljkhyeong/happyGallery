package com.personal.happygallery.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/** 후기 공개 상태가 실제로 전이될 때만 추가되는 불변 감사 기록. */
@Entity
@Table(
        name = "review_moderation_actions",
        indexes = {
                @Index(
                        name = "idx_review_moderation_review_created",
                        columnList = "review_id,created_at,id"),
                @Index(name = "idx_review_moderation_created", columnList = "created_at,id")
        }
)
public class ReviewModerationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReviewModerationActionType action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 10)
    private ReviewStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 10)
    private ReviewStatus newStatus;

    @Column(length = Review.MAX_HIDDEN_REASON_LENGTH)
    private String reason;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "evidence_snapshot_id")
    private Long evidenceSnapshotId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReviewModerationAction() {}

    private ReviewModerationAction(Long reviewId,
                                   ReviewModerationActionType action,
                                   ReviewStatus previousStatus,
                                   ReviewStatus newStatus,
                                   String reason,
                                   Long adminUserId,
                                   Long evidenceSnapshotId,
                                   LocalDateTime createdAt) {
        this.reviewId = requirePositive(reviewId, "후기 ID");
        this.action = Objects.requireNonNull(action, "운영 조치 유형은 필수입니다.");
        this.previousStatus = Objects.requireNonNull(previousStatus, "이전 상태는 필수입니다.");
        this.newStatus = Objects.requireNonNull(newStatus, "변경 상태는 필수입니다.");
        this.reason = reason;
        this.adminUserId = requirePositive(adminUserId, "관리자 ID");
        this.evidenceSnapshotId = evidenceSnapshotId;
        this.createdAt = Objects.requireNonNull(createdAt, "운영 조치 시각은 필수입니다.");
    }

    public static ReviewModerationAction hide(
            Long reviewId,
            String reason,
            Long adminUserId,
            Long evidenceSnapshotId,
            LocalDateTime createdAt) {
        return new ReviewModerationAction(
                reviewId,
                ReviewModerationActionType.HIDE,
                ReviewStatus.PUBLISHED,
                ReviewStatus.HIDDEN,
                Objects.requireNonNull(reason, "숨김 사유는 필수입니다."),
                adminUserId,
                requirePositive(evidenceSnapshotId, "후기 증거 ID"),
                createdAt);
    }

    public static ReviewModerationAction republish(
            Long reviewId,
            Long adminUserId,
            Long evidenceSnapshotId,
            LocalDateTime createdAt) {
        return new ReviewModerationAction(
                reviewId,
                ReviewModerationActionType.REPUBLISH,
                ReviewStatus.HIDDEN,
                ReviewStatus.PUBLISHED,
                null,
                adminUserId,
                requirePositive(evidenceSnapshotId, "후기 증거 ID"),
                createdAt);
    }

    /** V124 이전 감사 레코드 호환용이며 신규 운영 흐름에서는 사용하지 않는다. */
    public static ReviewModerationAction legacyHideWithoutEvidence(
            Long reviewId, String reason, Long adminUserId, LocalDateTime createdAt) {
        return new ReviewModerationAction(
                reviewId,
                ReviewModerationActionType.HIDE,
                ReviewStatus.PUBLISHED,
                ReviewStatus.HIDDEN,
                Objects.requireNonNull(reason, "숨김 사유는 필수입니다."),
                adminUserId,
                null,
                createdAt);
    }

    /** V124 이전 감사 레코드 호환용이며 신규 운영 흐름에서는 사용하지 않는다. */
    public static ReviewModerationAction legacyRepublishWithoutEvidence(
            Long reviewId, Long adminUserId, LocalDateTime createdAt) {
        return new ReviewModerationAction(
                reviewId,
                ReviewModerationActionType.REPUBLISH,
                ReviewStatus.HIDDEN,
                ReviewStatus.PUBLISHED,
                null,
                adminUserId,
                null,
                createdAt);
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value < 1L) {
            throw new IllegalArgumentException(name + "가 올바르지 않습니다.");
        }
        return value;
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public ReviewModerationActionType getAction() { return action; }
    public ReviewStatus getPreviousStatus() { return previousStatus; }
    public ReviewStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public Long getAdminUserId() { return adminUserId; }
    public Long getEvidenceSnapshotId() { return evidenceSnapshotId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
