package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.adapter.out.persistence.support.PersistenceConstraintNames;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.Review;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaReviewPersistenceAdapter implements ReviewStorePort {

    private static final String ORDER_ITEM_UNIQUE = "uq_reviews_reserved_order_item";
    private static final String BOOKING_UNIQUE = "uq_reviews_reserved_booking";

    private final ReviewRepository repository;

    JpaReviewPersistenceAdapter(ReviewRepository repository) {
        this.repository = repository;
    }

    @Override
    public Review save(Review review) {
        try {
            return repository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            if (PersistenceConstraintNames.matches(exception, ORDER_ITEM_UNIQUE)
                    || PersistenceConstraintNames.matches(exception, BOOKING_UNIQUE)) {
                throw new HappyGalleryException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

}
