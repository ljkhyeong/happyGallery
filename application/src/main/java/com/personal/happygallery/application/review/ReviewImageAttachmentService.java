package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이미지 파일 저장과 DB 참조 생성을 분리하기 위한 짧은 트랜잭션 경계. */
@Service
class ReviewImageAttachmentService {

    private final ReviewReaderPort reviewReader;
    private final ReviewImagePort imagePort;
    private final Clock clock;

    ReviewImageAttachmentService(
            ReviewReaderPort reviewReader, ReviewImagePort imagePort, Clock clock) {
        this.reviewReader = reviewReader;
        this.imagePort = imagePort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void validateCanAttach(Long userId, Long reviewId) {
        Review review = reviewReader.findByIdAndUserId(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requireActive();
        if (imagePort.countByReviewId(reviewId) >= ReviewImage.MAX_IMAGES) {
            throw new HappyGalleryException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED);
        }
    }

    @Transactional
    public ReviewImage attach(Long userId, Long reviewId, String imageUrl) {
        Review review = ownedReviewForUpdate(userId, reviewId);
        review.requireActive();
        var images = imagePort.findByReviewId(reviewId);
        if (images.size() >= ReviewImage.MAX_IMAGES) {
            throw new HappyGalleryException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED);
        }
        int sortOrder = java.util.stream.IntStream.range(0, ReviewImage.MAX_IMAGES)
                .filter(candidate -> images.stream()
                        .noneMatch(image -> image.getSortOrder() == candidate))
                .findFirst()
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED));
        return imagePort.save(new ReviewImage(
                reviewId, imageUrl, sortOrder, LocalDateTime.now(clock)));
    }

    @Transactional
    public void remove(Long userId, Long reviewId, Long imageId) {
        Review review = ownedReviewForUpdate(userId, reviewId);
        review.requireActive();
        ReviewImage image = imagePort.findByIdAndReviewId(imageId, reviewId)
                .orElseThrow(NotFoundException.supplier("후기 이미지"));
        imagePort.delete(image);
    }

    private Review ownedReviewForUpdate(Long userId, Long reviewId) {
        return reviewReader.findByIdAndUserIdForUpdate(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기"));
    }
}
