package com.personal.happygallery.domain.review;

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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

/** 회원의 후기 신고와 운영 결정을 보존한다. */
@Entity
@Table(
        name = "review_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_reports_review_reporter",
                columnNames = {"review_id", "reporter_user_id"}),
        indexes = {
                @Index(name = "idx_review_reports_status_created", columnList = "status,created_at,id"),
                @Index(name = "idx_review_reports_created", columnList = "created_at,id"),
                @Index(name = "idx_review_reports_decided", columnList = "decided_at,id")
        }
)
public class ReviewReport {

    public static final int MAX_DETAIL_LENGTH = 1_000;
    public static final int MAX_DECISION_NOTE_LENGTH = 1_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewReportReason reason;

    @Column(length = MAX_DETAIL_LENGTH)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_status", nullable = false, length = 10)
    private ReviewStatus snapshotStatus;

    @Column(name = "evidence_snapshot_id", nullable = false)
    private Long evidenceSnapshotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewReportStatus status;

    @Column(name = "decision_note", length = MAX_DECISION_NOTE_LENGTH)
    private String decisionNote;

    @Column(name = "decided_by_admin_id")
    private Long decidedByAdminId;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReviewReport() {}

    public ReviewReport(Long reviewId,
                        Long reporterUserId,
                        ReviewReportReason reason,
                        String detail,
                        ReviewStatus snapshotStatus,
                        Long evidenceSnapshotId,
                        LocalDateTime createdAt) {
        this.reviewId = requirePositive(reviewId, "후기 ID");
        this.reporterUserId = requirePositive(reporterUserId, "신고 회원 ID");
        this.reason = Objects.requireNonNull(reason, "신고 사유는 필수입니다.");
        this.detail = normalize(detail, MAX_DETAIL_LENGTH, "신고 상세");
        this.snapshotStatus = Objects.requireNonNull(snapshotStatus, "신고 시점 후기 상태는 필수입니다.");
        this.evidenceSnapshotId = requirePositive(evidenceSnapshotId, "후기 증거 ID");
        this.status = ReviewReportStatus.PENDING;
        this.createdAt = Objects.requireNonNull(createdAt, "신고 시각은 필수입니다.");
    }

    public void decide(ReviewReportStatus decision,
                       String decisionNote,
                       Long adminUserId,
                       LocalDateTime decidedAt) {
        if (status != ReviewReportStatus.PENDING
                || decision == null
                || decision == ReviewReportStatus.PENDING) {
            throw new HappyGalleryException(ErrorCode.REVIEW_REPORT_DECISION_NOT_ALLOWED);
        }
        this.status = decision;
        this.decisionNote = normalize(
                decisionNote, MAX_DECISION_NOTE_LENGTH, "신고 처리 메모");
        this.decidedByAdminId = requirePositive(adminUserId, "관리자 ID");
        this.decidedAt = Objects.requireNonNull(decidedAt, "신고 처리 시각은 필수입니다.");
    }

    private static Long requirePositive(Long value, String name) {
        if (value == null || value < 1L) {
            throw new IllegalArgumentException(name + "가 올바르지 않습니다.");
        }
        return value;
    }

    private static String normalize(String value, int maxLength, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, name + "는 " + maxLength + "자 이하여야 합니다.");
        }
        return normalized;
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public Long getReporterUserId() { return reporterUserId; }
    public ReviewReportReason getReason() { return reason; }
    public String getDetail() { return detail; }
    public ReviewStatus getSnapshotStatus() { return snapshotStatus; }
    public Long getEvidenceSnapshotId() { return evidenceSnapshotId; }
    public ReviewReportStatus getStatus() { return status; }
    public String getDecisionNote() { return decisionNote; }
    public Long getDecidedByAdminId() { return decidedByAdminId; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
