package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.domain.review.ReviewModerationAction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewModerationAdapter implements ReviewModerationPort {

    private final ReviewModerationActionRepository repository;

    JpaReviewModerationAdapter(ReviewModerationActionRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReviewModerationAction save(ReviewModerationAction action) {
        return repository.saveAndFlush(action);
    }

    @Override
    public List<ReviewModerationAction> findByReviewId(Long reviewId) {
        return repository.findByReviewIdOrderByCreatedAtAscIdAsc(reviewId);
    }

    @Override
    public List<ReviewModerationAction> findBefore(LocalDateTime cutoff, int limit) {
        return repository.findByCreatedAtLessThanEqualOrderByCreatedAtAscIdAsc(
                cutoff, PageRequest.ofSize(limit));
    }

    @Override
    public void deleteAll(List<ReviewModerationAction> actions) {
        repository.deleteAll(actions);
        repository.flush();
    }
}
