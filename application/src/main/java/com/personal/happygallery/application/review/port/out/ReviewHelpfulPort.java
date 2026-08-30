package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import java.util.List;

public interface ReviewHelpfulPort {

    void saveIfAbsent(ReviewHelpfulVote vote);

    void delete(Long reviewId, Long userId);

    long countByReviewId(Long reviewId);

    List<ReviewHelpfulCountView> countByReviewIds(List<Long> reviewIds);

    List<Long> findHelpfulReviewIds(Long userId, List<Long> reviewIds);

    record ReviewHelpfulCountView(Long reviewId, long helpfulCount) {}
}
