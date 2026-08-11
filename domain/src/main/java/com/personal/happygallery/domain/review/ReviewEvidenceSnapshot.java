package com.personal.happygallery.domain.review;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 신고와 운영 심사 시점의 후기 콘텐츠를 보존하는 불변 증거. */
@Entity
@Table(
        name = "review_evidence_snapshots",
        indexes = {
                @Index(name = "idx_review_evidence_review_revision", columnList = "review_id,content_revision,id"),
                @Index(name = "idx_review_evidence_retention", columnList = "retention_until,id")
        }
)
public class ReviewEvidenceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "content_revision", nullable = false)
    private long contentRevision;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewEvidenceProvenance provenance;

    @Column(name = "images_complete", nullable = false)
    private boolean imagesComplete;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "review_evidence_snapshot_images",
            joinColumns = @JoinColumn(name = "snapshot_id"),
            indexes = @Index(name = "idx_review_evidence_image_url", columnList = "image_url"))
    @OrderColumn(name = "sort_order")
    @Column(name = "image_url", nullable = false, length = ReviewImage.MAX_URL_LENGTH)
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "captured_at", nullable = false, updatable = false)
    private LocalDateTime capturedAt;

    /** null이면 미결 신고가 참조하므로 만료시키지 않는다. */
    @Column(name = "retention_until")
    private LocalDateTime retentionUntil;

    protected ReviewEvidenceSnapshot() {}

    public ReviewEvidenceSnapshot(
            Long reviewId,
            long contentRevision,
            int rating,
            String content,
            LocalDateTime editedAt,
            List<String> imageUrls,
            LocalDateTime capturedAt,
            LocalDateTime retentionUntil) {
        if (reviewId == null || reviewId < 1L) {
            throw new IllegalArgumentException("후기 ID가 올바르지 않습니다.");
        }
        if (contentRevision < 1L) {
            throw new IllegalArgumentException("후기 콘텐츠 revision이 올바르지 않습니다.");
        }
        if (rating < Review.MIN_RATING || rating > Review.MAX_RATING) {
            throw new IllegalArgumentException("후기 증거 별점이 올바르지 않습니다.");
        }
        this.reviewId = reviewId;
        this.contentRevision = contentRevision;
        this.rating = rating;
        this.content = Objects.requireNonNull(content, "후기 증거 내용은 필수입니다.");
        this.editedAt = editedAt;
        this.provenance = ReviewEvidenceProvenance.LIVE;
        this.imagesComplete = true;
        this.imageUrls = normalizeImages(imageUrls);
        this.capturedAt = Objects.requireNonNull(capturedAt, "후기 증거 생성 시각은 필수입니다.");
        this.retentionUntil = retentionUntil;
    }

    public void startRetention(LocalDateTime retentionUntil) {
        if (this.retentionUntil == null) {
            this.retentionUntil = Objects.requireNonNull(
                    retentionUntil, "후기 증거 만료 시각은 필수입니다.");
        }
    }

    private static List<String> normalizeImages(List<String> imageUrls) {
        List<String> normalized = imageUrls == null ? List.of() : imageUrls.stream()
                .map(url -> Objects.requireNonNull(url, "후기 증거 이미지 URL은 필수입니다.").strip())
                .toList();
        if (normalized.size() > ReviewImage.MAX_IMAGES
                || normalized.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("후기 증거 이미지가 올바르지 않습니다.");
        }
        return new ArrayList<>(normalized);
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public long getContentRevision() { return contentRevision; }
    public int getRating() { return rating; }
    public String getContent() { return content; }
    public LocalDateTime getEditedAt() { return editedAt; }
    public ReviewEvidenceProvenance getProvenance() { return provenance; }
    public boolean isImagesComplete() { return imagesComplete; }
    public List<String> getImageUrls() { return Collections.unmodifiableList(imageUrls); }
    public LocalDateTime getCapturedAt() { return capturedAt; }
    public LocalDateTime getRetentionUntil() { return retentionUntil; }
}
