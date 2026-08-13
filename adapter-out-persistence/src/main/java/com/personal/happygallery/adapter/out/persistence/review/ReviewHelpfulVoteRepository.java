package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort.ReviewHelpfulCountView;
import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewHelpfulVoteRepository extends JpaRepository<ReviewHelpfulVote, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO review_helpful_votes (review_id, user_id, created_at)
            VALUES (:reviewId, :userId, :createdAt)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId,
            @Param("createdAt") java.time.LocalDateTime createdAt);

    @Modifying
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewHelpfulPort$ReviewHelpfulCountView(
                v.reviewId, COUNT(v.id)
            )
            FROM ReviewHelpfulVote v
            WHERE v.reviewId IN :reviewIds
            GROUP BY v.reviewId
            """)
    List<ReviewHelpfulCountView> countRows(@Param("reviewIds") List<Long> reviewIds);

    @Query("""
            SELECT v.reviewId FROM ReviewHelpfulVote v
            WHERE v.userId = :userId AND v.reviewId IN :reviewIds
            """)
    List<Long> findHelpfulReviewIds(
            @Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}
