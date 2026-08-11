package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewTombstoneRetentionPort;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewTombstoneRetentionAdapter implements ReviewTombstoneRetentionPort {

    private final ReviewTombstoneRetentionRepository repository;

    JpaReviewTombstoneRetentionAdapter(ReviewTombstoneRetentionRepository repository) {
        this.repository = repository;
    }

    @Override
    public int deleteUnblockedBefore(LocalDateTime cutoff, int limit) {
        return repository.deleteUnblockedBefore(cutoff, limit);
    }
}
