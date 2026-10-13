package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.PublicReviewPage;
import com.personal.happygallery.domain.review.ReviewSort;

/** 상품·클래스 상세에 공개 후기를 제공하는 유스케이스. */
public interface PublicReviewUseCase {

    PublicReviewPage listProductReviews(
            Long productId, Integer rating, ReviewSort sort, String cursor, int size);

    PublicReviewPage listClassReviews(
            Long classId, Integer rating, ReviewSort sort, String cursor, int size);
}
