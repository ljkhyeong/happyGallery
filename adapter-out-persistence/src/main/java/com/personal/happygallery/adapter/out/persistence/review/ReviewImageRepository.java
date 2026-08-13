package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.domain.review.ReviewImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long>, ReviewImagePort {

    @Override
    <S extends ReviewImage> S save(S image);

    @Override
    Optional<ReviewImage> findByIdAndReviewId(Long imageId, Long reviewId);

    @Override
    @Query("SELECT i FROM ReviewImage i WHERE i.reviewId = :reviewId "
            + "ORDER BY i.sortOrder ASC, i.id ASC")
    List<ReviewImage> findByReviewId(@Param("reviewId") Long reviewId);

    @Query("SELECT i FROM ReviewImage i WHERE i.reviewId IN :reviewIds "
            + "ORDER BY i.reviewId ASC, i.sortOrder ASC, i.id ASC")
    List<ReviewImage> findByReviewIdRows(@Param("reviewIds") List<Long> reviewIds);

    @Override
    default List<ReviewImage> findByReviewIds(List<Long> reviewIds) {
        return reviewIds.isEmpty() ? List.of() : findByReviewIdRows(reviewIds);
    }

    @Override
    long countByReviewId(Long reviewId);

    @Override
    void delete(ReviewImage image);

    @Override
    void deleteByReviewId(Long reviewId);
}
