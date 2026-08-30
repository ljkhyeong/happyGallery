package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewImage;
import java.util.List;
import java.util.Optional;

public interface ReviewImagePort {

    <S extends ReviewImage> S save(S image);

    Optional<ReviewImage> findByIdAndReviewId(Long imageId, Long reviewId);

    List<ReviewImage> findByReviewId(Long reviewId);

    List<ReviewImage> findByReviewIds(List<Long> reviewIds);

    long countByReviewId(Long reviewId);

    void delete(ReviewImage image);

    void deleteByReviewId(Long reviewId);
}
