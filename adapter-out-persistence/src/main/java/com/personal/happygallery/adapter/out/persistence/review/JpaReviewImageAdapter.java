package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.domain.review.ReviewImage;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewImageAdapter implements ReviewImagePort {

    private final ReviewImageRepository repository;

    JpaReviewImageAdapter(ReviewImageRepository repository) {
        this.repository = repository;
    }

    @Override
    public ReviewImage save(ReviewImage image) {
        return repository.saveAndFlush(image);
    }

    @Override
    public Optional<ReviewImage> findByIdAndReviewId(Long imageId, Long reviewId) {
        return repository.findByIdAndReviewId(imageId, reviewId);
    }

    @Override
    public List<ReviewImage> findByReviewId(Long reviewId) {
        return repository.findByReviewIdOrderBySortOrderAscIdAsc(reviewId);
    }

    @Override
    public List<ReviewImage> findByReviewIds(List<Long> reviewIds) {
        return reviewIds.isEmpty()
                ? List.of()
                : repository.findByReviewIdInOrderByReviewIdAscSortOrderAscIdAsc(reviewIds);
    }

    @Override
    public long countByReviewId(Long reviewId) {
        return repository.countByReviewId(reviewId);
    }

    @Override
    public void delete(ReviewImage image) {
        repository.delete(image);
        repository.flush();
    }

    @Override
    public void deleteByReviewId(Long reviewId) {
        repository.deleteByReviewId(reviewId);
        repository.flush();
    }
}
