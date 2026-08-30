package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewTombstoneRetentionPort;
import com.personal.happygallery.domain.review.Review;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReviewTombstoneRetentionRepository
        extends Repository<Review, Long>, ReviewTombstoneRetentionPort {

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM reviews
            WHERE deleted_at <= :cutoff
              AND recreation_blocked = FALSE
              AND NOT EXISTS (
                  SELECT 1 FROM review_reports rr WHERE rr.review_id = reviews.id)
              AND NOT EXISTS (
                  SELECT 1 FROM review_moderation_actions rma
                  WHERE rma.review_id = reviews.id)
              AND NOT EXISTS (
                  SELECT 1 FROM review_evidence_snapshots res
                  WHERE res.review_id = reviews.id)
            ORDER BY deleted_at ASC, id ASC
            LIMIT :limit
            """, nativeQuery = true)
    int deleteUnblockedBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("limit") int limit);
}
