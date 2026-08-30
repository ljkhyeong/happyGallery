package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewModerationAction;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewModerationPort {

    <S extends ReviewModerationAction> S save(S action);

    List<ReviewModerationAction> findByReviewId(Long reviewId);

    List<ReviewModerationAction> findBefore(LocalDateTime cutoff, int limit);

    void deleteAll(Iterable<? extends ReviewModerationAction> actions);
}
