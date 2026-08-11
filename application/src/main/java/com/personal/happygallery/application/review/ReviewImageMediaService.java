package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.review.port.in.ReviewImageMediaUseCase;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import com.personal.happygallery.domain.review.ReviewStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReviewImageMediaService implements ReviewImageMediaUseCase {

    private final ReviewReaderPort reviewReader;
    private final ReviewImagePort imagePort;
    private final ImageMediaUseCase imageMediaUseCase;

    ReviewImageMediaService(
            ReviewReaderPort reviewReader,
            ReviewImagePort imagePort,
            ImageMediaUseCase imageMediaUseCase
    ) {
        this.reviewReader = reviewReader;
        this.imagePort = imagePort;
        this.imageMediaUseCase = imageMediaUseCase;
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewImageContent getOwnedImage(Long userId, Long reviewId, Long imageId) {
        Review review = reviewReader.findByIdAndUserId(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기 이미지"));
        review.requireActive();
        requireHidden(review.getStatus());
        return loadImage(reviewId, imageId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewImageContent getAdminImage(Long reviewId, Long imageId) {
        var review = reviewReader.findViewById(reviewId)
                .orElseThrow(NotFoundException.supplier("후기 이미지"));
        requireHidden(review.status());
        return loadImage(reviewId, imageId);
    }

    private ReviewImageContent loadImage(Long reviewId, Long imageId) {
        ReviewImage reviewImage = imagePort.findByIdAndReviewId(imageId, reviewId)
                .orElseThrow(NotFoundException.supplier("후기 이미지"));
        String fileName = ImageMediaReferenceGuard.localFileName(reviewImage.getImageUrl());
        if (fileName == null) {
            throw new NotFoundException("후기 이미지");
        }
        ImageMediaUseCase.ImageContent image = imageMediaUseCase.get(fileName);
        return new ReviewImageContent(image.bytes(), image.contentType());
    }

    private void requireHidden(ReviewStatus status) {
        if (status != ReviewStatus.HIDDEN) {
            throw new NotFoundException("후기 이미지");
        }
    }
}
