package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.review.ReviewImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    Optional<ReviewImage> findByIdAndReviewId(Long imageId, Long reviewId);

    List<ReviewImage> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

    List<ReviewImage> findByReviewIdInOrderByReviewIdAscSortOrderAscIdAsc(List<Long> reviewIds);

    long countByReviewId(Long reviewId);

    void deleteByReviewId(Long reviewId);
}
