package com.personal.happygallery.application.review.port.in;

import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewCreationState;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewImageItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewOpportunity;
import com.personal.happygallery.application.shared.page.CursorPage;
import java.util.List;

/** 회원 본인의 후기 작성·조회·수정 유스케이스. */
public interface MemberReviewUseCase {

    ReviewItem createProductReview(Long userId, Long orderItemId, int rating, String content);

    ReviewItem createClassReview(Long userId, Long bookingId, int rating, String content);

    ReviewItem updateReview(
            Long userId, Long reviewId, long expectedContentRevision, int rating, String content);

    void deleteReview(Long userId, Long reviewId);

    CursorPage<ReviewItem> listMyReviews(Long userId, String cursor, int size);

    List<ReviewItem> listMyOrderReviews(Long userId, Long orderId);

    List<ReviewItem> listMyBookingReviews(Long userId, Long bookingId);

    CursorPage<ReviewOpportunity> listMyReviewOpportunities(Long userId, String cursor, int size);

    ReviewCreationState getProductReviewCreationState(Long userId, Long orderItemId);

    ReviewCreationState getClassReviewCreationState(Long userId, Long bookingId);

    ReviewImageItem addReviewImage(Long userId, Long reviewId, byte[] bytes, String contentType);

    void deleteReviewImage(Long userId, Long reviewId, Long imageId);
}
