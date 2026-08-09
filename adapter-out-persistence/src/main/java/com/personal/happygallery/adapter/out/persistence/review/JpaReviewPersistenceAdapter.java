package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.review.Review;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
            if (hasConstraint(exception, ORDER_ITEM_UNIQUE)
                    || hasConstraint(exception, BOOKING_UNIQUE)) {
                throw new HappyGalleryException(ErrorCode.REVIEW_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    private static boolean hasConstraint(Throwable throwable, String expectedConstraint) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && StringUtils.hasText(violation.getConstraintName())) {
                String constraint = StringUtils.unqualify(violation.getConstraintName()
                        .toLowerCase(Locale.ROOT)
                        .replace("`", "")
                        .replace("\"", "")
                        .replace("'", ""));
                return expectedConstraint.equals(constraint);
            }
            current = current.getCause();
        }
        return false;
    }
}
