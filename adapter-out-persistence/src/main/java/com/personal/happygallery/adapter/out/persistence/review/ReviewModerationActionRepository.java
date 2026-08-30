package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.domain.review.ReviewModerationAction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewModerationActionRepository
        extends JpaRepository<ReviewModerationAction, Long>, ReviewModerationPort {

    @Override
    <S extends ReviewModerationAction> S save(S action);

    List<ReviewModerationAction> findByReviewIdOrderByCreatedAtAscIdAsc(Long reviewId);

    @Override
    default List<ReviewModerationAction> findByReviewId(Long reviewId) {
        return findByReviewIdOrderByCreatedAtAscIdAsc(reviewId);
    }

    List<ReviewModerationAction> findByCreatedAtLessThanEqualOrderByCreatedAtAscIdAsc(
            LocalDateTime cutoff, Pageable pageable);

    @Override
    default List<ReviewModerationAction> findBefore(LocalDateTime cutoff, int limit) {
        return findByCreatedAtLessThanEqualOrderByCreatedAtAscIdAsc(
                cutoff, PageRequest.ofSize(limit));
    }

    @Override
    void deleteAll(Iterable<? extends ReviewModerationAction> actions);
}
