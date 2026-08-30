package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort;
import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort.ReviewHelpfulCountView;
import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewHelpfulVoteRepository
        extends JpaRepository<ReviewHelpfulVote, Long>, ReviewHelpfulPort {

    @Override
    default void saveIfAbsent(ReviewHelpfulVote vote) {
        insertIfAbsent(vote.getReviewId(), vote.getUserId(), vote.getCreatedAt());
    }

    @Modifying
    @Query(value = """
            INSERT INTO review_helpful_votes (review_id, user_id, created_at)
            VALUES (:reviewId, :userId, :createdAt)
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("reviewId") Long reviewId,
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt);

    @Modifying
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);

    @Override
    default void delete(Long reviewId, Long userId) {
        deleteByReviewIdAndUserId(reviewId, userId);
    }

    @Override
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

    @Override
    default List<ReviewHelpfulCountView> countByReviewIds(List<Long> reviewIds) {
        return reviewIds.isEmpty() ? List.of() : countRows(reviewIds);
    }

    @Query("""
            SELECT v.reviewId FROM ReviewHelpfulVote v
            WHERE v.userId = :userId AND v.reviewId IN :reviewIds
            """)
    List<Long> findHelpfulReviewIdRows(
            @Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);

    @Override
    default List<Long> findHelpfulReviewIds(Long userId, List<Long> reviewIds) {
        return reviewIds.isEmpty() ? List.of() : findHelpfulReviewIdRows(userId, reviewIds);
    }
}
