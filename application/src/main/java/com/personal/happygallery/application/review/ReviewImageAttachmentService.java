package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaReferenceGuard;
import com.personal.happygallery.application.media.ImageMediaReferenceRemovedEvent;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewImage;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이미지 파일 저장과 DB 참조 생성을 분리하기 위한 짧은 트랜잭션 경계. */
@Service
class ReviewImageAttachmentService {

    private final ReviewReaderPort reviewReader;
    private final ReviewStorePort reviewStore;
    private final ReviewImagePort imagePort;
    private final ImageMediaReferenceGuard referenceGuard;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    ReviewImageAttachmentService(
            ReviewReaderPort reviewReader,
            ReviewStorePort reviewStore,
            ReviewImagePort imagePort,
            ImageMediaReferenceGuard referenceGuard,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.reviewReader = reviewReader;
        this.reviewStore = reviewStore;
        this.imagePort = imagePort;
        this.referenceGuard = referenceGuard;
        this.eventPublisher = eventPublisher;
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
        referenceGuard.validateAssignment(imageUrl);
        int sortOrder = IntStream.range(0, ReviewImage.MAX_IMAGES)
                .filter(candidate -> images.stream()
                        .noneMatch(image -> image.getSortOrder() == candidate))
                .findFirst()
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED));
        LocalDateTime changedAt = LocalDateTime.now(clock);
        ReviewImage saved = imagePort.save(new ReviewImage(
                reviewId, imageUrl, sortOrder, changedAt));
        review.recordContentChange(changedAt);
        reviewStore.save(review);
        return saved;
    }

    @Transactional
    public void remove(Long userId, Long reviewId, Long imageId) {
        Review review = ownedReviewForUpdate(userId, reviewId);
        review.requireActive();
        ReviewImage image = imagePort.findByIdAndReviewId(imageId, reviewId)
                .orElseThrow(NotFoundException.supplier("후기 이미지"));
        imagePort.delete(image);
        review.recordContentChange(LocalDateTime.now(clock));
        reviewStore.save(review);
        publishReferencesRemoved(List.of(image.getImageUrl()));
    }

    @Transactional
    public void removeAll(Long reviewId) {
        List<String> imageUrls = imagePort.findByReviewId(reviewId).stream()
                .map(ReviewImage::getImageUrl)
                .toList();
        imagePort.deleteByReviewId(reviewId);
        publishReferencesRemoved(imageUrls);
    }

    private Review ownedReviewForUpdate(Long userId, Long reviewId) {
        return reviewReader.findByIdAndUserIdForUpdate(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기"));
    }

    private void publishReferencesRemoved(List<String> imageUrls) {
        if (!imageUrls.isEmpty()) {
            eventPublisher.publishEvent(new ImageMediaReferenceRemovedEvent(imageUrls));
        }
    }
}
