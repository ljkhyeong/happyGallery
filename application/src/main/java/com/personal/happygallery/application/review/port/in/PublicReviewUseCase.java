package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.PublicReviewPage;
import com.personal.happygallery.domain.review.ReviewSort;

/** 상품·클래스 상세에 공개 후기를 제공하는 유스케이스. */
public interface PublicReviewUseCase {

    default PublicReviewPage listProductReviews(Long productId, String cursor, int size) {
        return listProductReviews(productId, null, ReviewSort.LATEST, cursor, size);
    }

    PublicReviewPage listProductReviews(
            Long productId, Integer rating, ReviewSort sort, String cursor, int size);

    default PublicReviewPage listClassReviews(Long classId, String cursor, int size) {
        return listClassReviews(classId, null, ReviewSort.LATEST, cursor, size);
    }

    PublicReviewPage listClassReviews(
            Long classId, Integer rating, ReviewSort sort, String cursor, int size);
}
