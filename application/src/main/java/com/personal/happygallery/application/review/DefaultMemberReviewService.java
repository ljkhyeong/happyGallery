package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.in.MemberReviewUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewCreationState;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewImageItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewOpportunity;
import com.personal.happygallery.application.review.port.out.ReviewEligibilityPort;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewOpportunityView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewSourceReservationView;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultMemberReviewService implements MemberReviewUseCase {

    private final ReviewReaderPort reviewReader;
    private final ReviewStorePort reviewStore;
    private final ReviewEligibilityPort eligibilityPort;
    private final ReviewImageAttachmentService imageAttachmentService;
    private final ReviewImageUploadService imageUploadService;
    private final ReviewViewAssembler viewAssembler;
    private final Clock clock;

    DefaultMemberReviewService(ReviewReaderPort reviewReader,
                               ReviewStorePort reviewStore,
                               ReviewEligibilityPort eligibilityPort,
                               ReviewImageAttachmentService imageAttachmentService,
                               ReviewImageUploadService imageUploadService,
                               ReviewViewAssembler viewAssembler,
                               Clock clock) {
        this.reviewReader = reviewReader;
        this.reviewStore = reviewStore;
        this.eligibilityPort = eligibilityPort;
        this.imageAttachmentService = imageAttachmentService;
        this.imageUploadService = imageUploadService;
        this.viewAssembler = viewAssembler;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ReviewItem createProductReview(
            Long userId, Long orderItemId, int rating, String content) {
        ReviewEligibilityPort.ProductReviewSource source = eligibilityPort
                .findOwnedProductSource(userId, orderItemId)
                .orElseThrow(NotFoundException.supplier("주문 품목"));
        source.orderStatus().requireReviewable();
        requireSourceAvailable(eligibilityPort.findProductSourceReservation(source.orderItemId()));
        Review review = Review.forProduct(
                userId,
                source.orderItemId(),
                source.productId(),
                rating,
                content,
                LocalDateTime.now(clock));
        return viewAssembler.savedView(reviewStore.save(review), false);
    }

    @Override
    @Transactional
    public ReviewItem createClassReview(
            Long userId, Long bookingId, int rating, String content) {
        ReviewEligibilityPort.ClassReviewSource source = eligibilityPort
                .findOwnedClassSource(userId, bookingId)
                .orElseThrow(NotFoundException.supplier("예약"));
        source.bookingStatus().requireReviewable();
        requireSourceAvailable(eligibilityPort.findClassSourceReservation(source.bookingId()));
        Review review = Review.forClass(
                userId,
                source.bookingId(),
                source.bookingClassId(),
                rating,
                content,
                LocalDateTime.now(clock));
        return viewAssembler.savedView(reviewStore.save(review), false);
    }

    @Override
    @Transactional
    public ReviewItem updateReview(
            Long userId,
            Long reviewId,
            long expectedContentRevision,
            int rating,
            String content) {
        Review review = ownedReviewForUpdate(userId, reviewId);
        review.requireContentRevision(expectedContentRevision);
        review.update(rating, content, LocalDateTime.now(clock));
        return viewAssembler.savedView(reviewStore.save(review), false);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = ownedReviewForUpdate(userId, reviewId);
        imageAttachmentService.removeAll(reviewId);
        review.softDelete(LocalDateTime.now(clock));
        reviewStore.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewItem> listMyReviews(Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        List<ReviewListView> fetched;
        if (cursor == null) {
            fetched = reviewReader.findByUserId(userId, pageSize + 1);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            fetched = reviewReader.findByUserIdAfter(
                    userId, cursorParam.timestamp(), cursorParam.id(), pageSize + 1);
        }
        return viewAssembler.standardPage(fetched, pageSize, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> listMyOrderReviews(Long userId, Long orderId) {
        if (!eligibilityPort.existsOwnedOrder(userId, orderId)) {
            throw new NotFoundException("주문");
        }
        return viewAssembler.toItems(reviewReader.findByOwnedOrder(userId, orderId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> listMyBookingReviews(Long userId, Long bookingId) {
        if (!eligibilityPort.existsOwnedBooking(userId, bookingId)) {
            throw new NotFoundException("예약");
        }
        return viewAssembler.toItems(reviewReader.findByOwnedBooking(userId, bookingId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewOpportunity> listMyReviewOpportunities(
            Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        ReviewOpportunityCursor.CursorParam cursorParam = cursor == null
                ? null
                : ReviewOpportunityCursor.decode(cursor);
        List<ReviewOpportunity> fetched = eligibilityPort.findReviewOpportunities(
                        userId,
                        cursorParam == null ? null : cursorParam.completedAt(),
                        cursorParam == null ? null : cursorParam.targetType(),
                        cursorParam == null ? null : cursorParam.sourceId(),
                        pageSize + 1).stream()
                .map(DefaultMemberReviewService::toOpportunity)
                .toList();
        return CursorPage.of(
                fetched,
                pageSize,
                item -> ReviewOpportunityCursor.encode(
                        item.completedAt(), item.targetType(), item.sourceId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewCreationState getProductReviewCreationState(Long userId, Long orderItemId) {
        ReviewEligibilityPort.ProductReviewSource source = eligibilityPort
                .findOwnedProductSource(userId, orderItemId)
                .orElseThrow(NotFoundException.supplier("주문 품목"));
        return new ReviewCreationState(
                ReviewTargetType.PRODUCT,
                orderItemId,
                creationStatus(
                        source.orderStatus().isReviewable(),
                        eligibilityPort.findProductSourceReservation(orderItemId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewCreationState getClassReviewCreationState(Long userId, Long bookingId) {
        ReviewEligibilityPort.ClassReviewSource source = eligibilityPort
                .findOwnedClassSource(userId, bookingId)
                .orElseThrow(NotFoundException.supplier("예약"));
        return new ReviewCreationState(
                ReviewTargetType.CLASS,
                bookingId,
                creationStatus(
                        source.bookingStatus().isReviewable(),
                        eligibilityPort.findClassSourceReservation(bookingId)));
    }

    @Override
    public ReviewImageItem addReviewImage(
            Long userId, Long reviewId, byte[] bytes, String contentType) {
        return ReviewViewAssembler.toImageItem(imageUploadService.upload(
                userId, reviewId, bytes, contentType));
    }

    @Override
    public void deleteReviewImage(Long userId, Long reviewId, Long imageId) {
        imageAttachmentService.remove(userId, reviewId, imageId);
    }

    private Review ownedReviewForUpdate(Long userId, Long reviewId) {
        return reviewReader.findByIdAndUserIdForUpdate(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기"));
    }

    private static void requireSourceAvailable(
            Optional<ReviewSourceReservationView> reservation) {
        reservation.ifPresent(existing -> {
            if (!existing.active() && existing.recreationBlocked()) {
                throw new HappyGalleryException(ErrorCode.REVIEW_RECREATION_BLOCKED);
            }
            throw new HappyGalleryException(ErrorCode.REVIEW_ALREADY_EXISTS);
        });
    }

    private static ReviewOpportunity toOpportunity(ReviewOpportunityView view) {
        return new ReviewOpportunity(
                view.targetType(),
                view.sourceId(),
                view.targetId(),
                view.targetName(),
                view.orderId(),
                view.bookingId(),
                view.completedAt());
    }

    private static ReviewCreationStatus creationStatus(
            boolean reviewable,
            Optional<ReviewSourceReservationView> reservation) {
        if (!reviewable) {
            return ReviewCreationStatus.NOT_REVIEWABLE;
        }
        return reservation
                .map(existing -> existing.active()
                        ? ReviewCreationStatus.REVIEW_EXISTS
                        : ReviewCreationStatus.RECREATION_BLOCKED)
                .orElse(ReviewCreationStatus.AVAILABLE);
    }
}
