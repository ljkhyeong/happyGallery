package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewEvidencePort {

    ReviewEvidenceSnapshot save(ReviewEvidenceSnapshot snapshot);

    Optional<ReviewEvidenceSnapshot> findById(Long snapshotId);

    List<ReviewEvidenceSnapshot> findByIds(List<Long> snapshotIds);

    List<ReviewEvidenceSnapshot> findExpired(LocalDateTime now, int limit);

    void deleteAll(List<ReviewEvidenceSnapshot> snapshots);
}
