package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewEvidenceSnapshotRepository
        extends JpaRepository<ReviewEvidenceSnapshot, Long>, ReviewEvidencePort {

    @Override
    <S extends ReviewEvidenceSnapshot> S save(S snapshot);

    @Override
    Optional<ReviewEvidenceSnapshot> findById(Long snapshotId);

    @EntityGraph(attributePaths = "imageUrls")
    @Query("SELECT DISTINCT s FROM ReviewEvidenceSnapshot s WHERE s.id IN :ids")
    List<ReviewEvidenceSnapshot> findWithImagesByIdIn(@Param("ids") List<Long> ids);

    @Override
    default List<ReviewEvidenceSnapshot> findByIds(List<Long> snapshotIds) {
        return snapshotIds.isEmpty() ? List.of() : findWithImagesByIdIn(snapshotIds);
    }

    @Query("""
            SELECT s FROM ReviewEvidenceSnapshot s
            WHERE s.retentionUntil IS NOT NULL
              AND s.retentionUntil <= :now
            ORDER BY s.retentionUntil ASC, s.id ASC
            """)
    List<ReviewEvidenceSnapshot> findExpiredPage(
            @Param("now") LocalDateTime now, Pageable pageable);

    @Override
    default List<ReviewEvidenceSnapshot> findExpired(LocalDateTime now, int limit) {
        return findExpiredPage(now, PageRequest.ofSize(limit));
    }

    @Override
    void deleteAll(Iterable<? extends ReviewEvidenceSnapshot> snapshots);
}
