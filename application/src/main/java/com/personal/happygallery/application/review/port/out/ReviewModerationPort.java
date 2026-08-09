package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewModerationAction;
import java.util.List;

public interface ReviewModerationPort {

    ReviewModerationAction save(ReviewModerationAction action);

    List<ReviewModerationAction> findByReviewId(Long reviewId);
}
