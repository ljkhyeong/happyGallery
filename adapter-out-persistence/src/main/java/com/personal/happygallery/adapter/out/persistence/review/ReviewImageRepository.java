package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.domain.review.ReviewImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long>, ReviewImagePort {

    @Override
    <S extends ReviewImage> S save(S image);

    @Override
    Optional<ReviewImage> findByIdAndReviewId(Long imageId, Long reviewId);

    List<ReviewImage> findByReviewIdOrderBySortOrderAscIdAsc(Long reviewId);

    @Override
    default List<ReviewImage> findByReviewId(Long reviewId) {
        return findByReviewIdOrderBySortOrderAscIdAsc(reviewId);
    }

    List<ReviewImage> findByReviewIdInOrderByReviewIdAscSortOrderAscIdAsc(
            List<Long> reviewIds);

    @Override
    default List<ReviewImage> findByReviewIds(List<Long> reviewIds) {
        return reviewIds.isEmpty()
                ? List.of()
                : findByReviewIdInOrderByReviewIdAscSortOrderAscIdAsc(reviewIds);
    }

    @Override
    long countByReviewId(Long reviewId);

    @Override
    void delete(ReviewImage image);

    @Override
    void deleteByReviewId(Long reviewId);
}
