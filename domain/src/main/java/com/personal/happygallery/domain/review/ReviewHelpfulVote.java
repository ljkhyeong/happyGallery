package com.personal.happygallery.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

/** 회원별 도움돼요 반응. 유일 제약으로 PUT 의미의 멱등성을 보조한다. */
@Entity
@Table(
        name = "review_helpful_votes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_review_helpful_review_user",
                columnNames = {"review_id", "user_id"})
)
public class ReviewHelpfulVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ReviewHelpfulVote() {}

    public ReviewHelpfulVote(Long reviewId, Long userId, LocalDateTime createdAt) {
        if (reviewId == null || reviewId < 1L || userId == null || userId < 1L) {
            throw new IllegalArgumentException("후기와 회원 ID가 올바르지 않습니다.");
        }
        this.reviewId = reviewId;
        this.userId = userId;
        this.createdAt = Objects.requireNonNull(createdAt, "도움돼요 시각은 필수입니다.");
    }

    public Long getId() { return id; }
    public Long getReviewId() { return reviewId; }
    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
