package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewEvidenceAdapter implements ReviewEvidencePort {

    private final ReviewEvidenceSnapshotRepository repository;

    JpaReviewEvidenceAdapter(ReviewEvidenceSnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReviewEvidenceSnapshot save(ReviewEvidenceSnapshot snapshot) {
        return repository.saveAndFlush(snapshot);
    }

    @Override
    public Optional<ReviewEvidenceSnapshot> findById(Long snapshotId) {
        return repository.findById(snapshotId);
    }

    @Override
    public List<ReviewEvidenceSnapshot> findByIds(List<Long> snapshotIds) {
        return snapshotIds.isEmpty() ? List.of() : repository.findWithImagesByIdIn(snapshotIds);
    }

    @Override
    public List<ReviewEvidenceSnapshot> findExpired(LocalDateTime now, int limit) {
        return repository.findExpired(now, PageRequest.ofSize(limit));
    }

    @Override
    public void deleteAll(List<ReviewEvidenceSnapshot> snapshots) {
        repository.deleteAll(snapshots);
        repository.flush();
    }
}
