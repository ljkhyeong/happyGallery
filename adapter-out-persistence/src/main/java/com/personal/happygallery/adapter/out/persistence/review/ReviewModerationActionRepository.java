package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.review.ReviewModerationAction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewModerationActionRepository
        extends JpaRepository<ReviewModerationAction, Long> {

    List<ReviewModerationAction> findByReviewIdOrderByCreatedAtAscIdAsc(Long reviewId);
}
