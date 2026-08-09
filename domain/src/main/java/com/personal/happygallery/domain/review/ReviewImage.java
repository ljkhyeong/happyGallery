package com.personal.happygallery.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

/** 후기 작성 뒤 소유자가 첨부하는 이미지 참조. */
@Entity
@Table(
        name = "review_images",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uq_review_images_order", columnNames = {"review_id", "sort_order"}),
            @UniqueConstraint(name = "uq_review_images_url", columnNames = "image_url")
        },
        indexes = @Index(name = "idx_review_images_review", columnList = "review_id,sort_order,id")
)
public class ReviewImage {

    public static final int MAX_IMAGES = 5;
    public static final int MAX_URL_LENGTH = 512;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "image_url", nullable = false, length = MAX_URL_LENGTH)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReviewImage() {}

    public ReviewImage(Long reviewId, String imageUrl, int sortOrder, LocalDateTime createdAt) {
        if (reviewId == null || reviewId < 1L) {
            throw new IllegalArgumentException("후기 ID가 올바르지 않습니다.");
        }
        if (imageUrl == null || imageUrl.isBlank() || imageUrl.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("후기 이미지 URL이 올바르지 않습니다.");
        }
        if (sortOrder < 0 || sortOrder >= MAX_IMAGES) {
            throw new IllegalArgumentException("후기 이미지 순서가 올바르지 않습니다.");
        }
        this.reviewId = reviewId;
        this.imageUrl = imageUrl.strip();
        this.sortOrder = sortOrder;
        this.createdAt = Objects.requireNonNull(createdAt, "후기 이미지 등록 시각은 필수입니다.");
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public String getImageUrl() { return imageUrl; }
    public int getSortOrder() { return sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
