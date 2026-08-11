package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewEvidenceSnapshotRepository
        extends JpaRepository<ReviewEvidenceSnapshot, Long> {

    @EntityGraph(attributePaths = "imageUrls")
    @Query("SELECT DISTINCT s FROM ReviewEvidenceSnapshot s WHERE s.id IN :ids")
    List<ReviewEvidenceSnapshot> findWithImagesByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            SELECT s FROM ReviewEvidenceSnapshot s
            WHERE s.retentionUntil IS NOT NULL
              AND s.retentionUntil <= :now
            ORDER BY s.retentionUntil ASC, s.id ASC
            """)
    List<ReviewEvidenceSnapshot> findExpired(
            @Param("now") LocalDateTime now, Pageable pageable);
}
