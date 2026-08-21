package com.personal.happygallery.application.review;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.notification.ReviewNotificationPublisher;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.application.review.port.out.ReviewEligibilityPort;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.application.review.port.out.ReviewInteractionStateView;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.application.review.port.out.ReviewOpportunityView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import com.personal.happygallery.application.review.port.out.ReviewReportListView;
import com.personal.happygallery.application.review.port.out.ReviewSourceReservationView;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.application.review.port.out.ReviewSummaryView;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewCreationStatus;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import com.personal.happygallery.domain.review.ReviewImage;
import com.personal.happygallery.domain.review.ReviewModerationAction;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewSort;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultReviewService implements ReviewUseCase {


    private final ReviewReaderPort reviewReader;
    private final ReviewStorePort reviewStore;
    private final ReviewEligibilityPort eligibilityPort;
    private final ReviewEvidencePort evidencePort;
    private final ReviewModerationPort moderationPort;
    private final ReviewReportPort reportPort;
    private final ReviewHelpfulPort helpfulPort;
    private final ReviewImagePort imagePort;
    private final ReviewEvidenceService evidenceService;
    private final ReviewImageAttachmentService imageAttachmentService;
    private final ReviewImageUploadService imageUploadService;
    private final ProductReaderPort productReader;
    private final ClassReaderPort classReader;
    private final UserReaderPort userReader;
    private final ReviewNotificationPublisher notificationPublisher;
    private final Clock clock;

    public DefaultReviewService(ReviewReaderPort reviewReader,
                                ReviewStorePort reviewStore,
                                ReviewEligibilityPort eligibilityPort,
                                ReviewEvidencePort evidencePort,
                                ReviewModerationPort moderationPort,
                                ReviewReportPort reportPort,
                                ReviewHelpfulPort helpfulPort,
                                ReviewImagePort imagePort,
                                ReviewEvidenceService evidenceService,
                                ReviewImageAttachmentService imageAttachmentService,
                                ReviewImageUploadService imageUploadService,
                                ProductReaderPort productReader,
                                ClassReaderPort classReader,
                                UserReaderPort userReader,
                                ReviewNotificationPublisher notificationPublisher,
                                Clock clock) {
        this.reviewReader = reviewReader;
        this.reviewStore = reviewStore;
        this.eligibilityPort = eligibilityPort;
        this.evidencePort = evidencePort;
        this.moderationPort = moderationPort;
        this.reportPort = reportPort;
        this.helpfulPort = helpfulPort;
        this.imagePort = imagePort;
        this.evidenceService = evidenceService;
        this.imageAttachmentService = imageAttachmentService;
        this.imageUploadService = imageUploadService;
        this.productReader = productReader;
        this.classReader = classReader;
        this.userReader = userReader;
        this.notificationPublisher = notificationPublisher;
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
        requireProductSourceAvailable(source.orderItemId());
        Review review = Review.forProduct(
                userId,
                source.orderItemId(),
                source.productId(),
                rating,
                content,
                LocalDateTime.now(clock));
        return savedView(reviewStore.save(review), false);
    }

    @Override
    @Transactional
    public ReviewItem createClassReview(
            Long userId, Long bookingId, int rating, String content) {
        ReviewEligibilityPort.ClassReviewSource source = eligibilityPort
                .findOwnedClassSource(userId, bookingId)
                .orElseThrow(NotFoundException.supplier("예약"));
        source.bookingStatus().requireReviewable();
        requireClassSourceAvailable(source.bookingId());
        Review review = Review.forClass(
                userId,
                source.bookingId(),
                source.bookingClassId(),
                rating,
                content,
                LocalDateTime.now(clock));
        return savedView(reviewStore.save(review), false);
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
        return savedView(reviewStore.save(review), false);
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
    public PublicReviewPage listProductReviews(
            Long productId, Integer rating, ReviewSort sort, String cursor, int size) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        int pageSize = PageParams.requireSize(size);
        ReviewPublicCursor.CursorParam cursorParam = decodeCursor(cursor, sort, rating);
        List<ReviewListView> fetched = reviewReader.findPublishedByProduct(
                productId,
                rating,
                sort,
                cursorParam == null ? null : cursorParam.rating(),
                cursorParam == null ? null : cursorParam.createdAt(),
                cursorParam == null ? null : cursorParam.id(),
                pageSize + 1);
        return publicPage(
                reviewReader.summarizePublishedProduct(productId),
                reviewReader.countPublishedProduct(productId, rating),
                fetched,
                pageSize,
                sort,
                rating);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicReviewPage listClassReviews(
            Long classId, Integer rating, ReviewSort sort, String cursor, int size) {
        classReader.findById(classId)
                .orElseThrow(NotFoundException.supplier("클래스"))
                .requireActive();
        int pageSize = PageParams.requireSize(size);
        ReviewPublicCursor.CursorParam cursorParam = decodeCursor(cursor, sort, rating);
        List<ReviewListView> fetched = reviewReader.findPublishedByClass(
                classId,
                rating,
                sort,
                cursorParam == null ? null : cursorParam.rating(),
                cursorParam == null ? null : cursorParam.createdAt(),
                cursorParam == null ? null : cursorParam.id(),
                pageSize + 1);
        return publicPage(
                reviewReader.summarizePublishedClass(classId),
                reviewReader.countPublishedClass(classId, rating),
                fetched,
                pageSize,
                sort,
                rating);
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
        return standardPage(fetched, pageSize, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> listMyOrderReviews(Long userId, Long orderId) {
        if (!eligibilityPort.existsOwnedOrder(userId, orderId)) {
            throw new NotFoundException("주문");
        }
        return toItems(reviewReader.findByOwnedOrder(userId, orderId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewItem> listMyBookingReviews(Long userId, Long bookingId) {
        if (!eligibilityPort.existsOwnedBooking(userId, bookingId)) {
            throw new NotFoundException("예약");
        }
        return toItems(reviewReader.findByOwnedBooking(userId, bookingId), false);
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
                .map(DefaultReviewService::toOpportunity)
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
    @Transactional(readOnly = true)
    public CursorPage<ReviewItem> listAdminReviews(
            ReviewTargetType targetType,
            ReviewStatus status,
            String cursor,
            int size) {
        int pageSize = PageParams.requireSize(size);
        List<ReviewListView> fetched;
        if (cursor == null) {
            fetched = reviewReader.findForAdmin(targetType, status, pageSize + 1);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            fetched = reviewReader.findForAdminAfter(
                    targetType,
                    status,
                    cursorParam.timestamp(),
                    cursorParam.id(),
                    pageSize + 1);
        }
        return standardPage(fetched, pageSize, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewItem getAdminReview(Long reviewId) {
        ReviewListView view = reviewReader.findViewById(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        return toItems(List.of(view), true).getFirst();
    }

    @Override
    @Transactional
    public ReviewItem updateStatus(
            Long reviewId,
            ReviewStatus status,
            String reason,
            long expectedContentRevision,
            long expectedVersion,
            Long adminUserId) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requireContentRevision(expectedContentRevision);
        review.requireVersion(expectedVersion);
        ReviewStatus previous = review.getStatus();
        LocalDateTime now = LocalDateTime.now(clock);
        if (previous == status) {
            return savedView(review, true);
        }
        ReviewEvidenceSnapshot evidence = evidenceService.captureForModeration(review);
        review.changeStatus(status, reason, adminUserId, now);
        reviewStore.save(review);
        ReviewModerationAction action = status == ReviewStatus.HIDDEN
                ? ReviewModerationAction.hide(
                        reviewId,
                        review.getHiddenReason(),
                        adminUserId,
                        evidence.getId(),
                        now)
                : ReviewModerationAction.republish(
                        reviewId, adminUserId, evidence.getId(), now);
        action = moderationPort.save(action);
        if (previous == ReviewStatus.PUBLISHED) {
            notificationPublisher.publishHidden(review.getUserId(), action.getId());
        } else {
            notificationPublisher.publishRepublished(review.getUserId(), action.getId());
        }
        return savedView(review, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationActionItem> listModerationActions(Long reviewId) {
        var actions = moderationPort.findByReviewId(reviewId);
        Map<Long, ReviewEvidenceSnapshot> evidenceById = evidenceById(actions.stream()
                .map(ReviewModerationAction::getEvidenceSnapshotId)
                .toList());
        return actions.stream()
                .map(action -> new ModerationActionItem(
                        action.getId(),
                        action.getReviewId(),
                        action.getAction(),
                        action.getPreviousStatus(),
                        action.getNewStatus(),
                        action.getReason(),
                        action.getAdminUserId(),
                        toEvidenceItem(evidenceById.get(action.getEvidenceSnapshotId())),
                        action.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public ReviewItem upsertOfficialReply(
            Long reviewId, String content, long expectedVersion, Long adminUserId) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requireVersion(expectedVersion);
        boolean created = review.upsertOfficialReply(
                content, adminUserId, LocalDateTime.now(clock));
        reviewStore.save(review);
        if (created) {
            notificationPublisher.publishOwnerReplied(review.getUserId(), reviewId);
        }
        return savedView(review, true);
    }

    @Override
    @Transactional
    public ReviewItem deleteOfficialReply(Long reviewId, long expectedVersion) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requireVersion(expectedVersion);
        review.removeOfficialReply(LocalDateTime.now(clock));
        reviewStore.save(review);
        return savedView(review, true);
    }

    @Override
    @Transactional
    public ReviewReportItem createReport(
            Long userId, Long reviewId, ReviewReportReason reason, String detail) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requirePublicInteraction(userId);
        if (reportPort.existsByReviewIdAndReporterUserId(reviewId, userId)) {
            throw new HappyGalleryException(ErrorCode.REVIEW_REPORT_ALREADY_EXISTS);
        }
        ReviewEvidenceSnapshot evidence = evidenceService.captureForPendingReport(review);
        ReviewReport report = new ReviewReport(
                reviewId,
                userId,
                reason,
                detail,
                review.getStatus(),
                evidence.getId(),
                LocalDateTime.now(clock));
        return toReportItem(reportPort.save(report), evidence);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewReportSummaryItem> listAdminReports(
            ReviewReportStatus status, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        List<ReviewReportListView> fetched;
        if (cursor == null) {
            fetched = reportPort.findForAdmin(status, pageSize + 1);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            fetched = reportPort.findForAdminAfter(
                    status, cursorParam.timestamp(), cursorParam.id(), pageSize + 1);
        }
        List<ReviewReportSummaryItem> items = fetched.stream()
                .map(DefaultReviewService::toReportSummaryItem)
                .toList();
        return CursorPage.of(
                items,
                pageSize,
                item -> CursorUtils.encode(item.createdAt(), item.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewReportItem getAdminReport(Long reportId) {
        ReviewReport report = reportPort.findById(reportId)
                .orElseThrow(NotFoundException.supplier("후기 신고"));
        ReviewEvidenceSnapshot evidence = evidencePort.findById(report.getEvidenceSnapshotId())
                .orElseThrow(NotFoundException.supplier("후기 증거"));
        return toReportItem(report, evidence);
    }

    @Override
    @Transactional
    public ReviewReportItem decideReport(
            Long reportId,
            ReviewReportStatus decision,
            String decisionNote,
            Long adminUserId) {
        ReviewReport report = reportPort.findByIdForUpdate(reportId)
                .orElseThrow(NotFoundException.supplier("후기 신고"));
        LocalDateTime now = LocalDateTime.now(clock);
        report.decide(decision, decisionNote, adminUserId, now);
        evidenceService.startResolvedReportRetention(report.getEvidenceSnapshotId(), now);
        ReviewReport saved = reportPort.save(report);
        ReviewEvidenceSnapshot evidence = evidencePort.findById(saved.getEvidenceSnapshotId())
                .orElseThrow(NotFoundException.supplier("후기 증거"));
        return toReportItem(saved, evidence);
    }

    @Override
    @Transactional
    public HelpfulResult markHelpful(Long userId, Long reviewId) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requirePublicInteraction(userId);
        helpfulPort.saveIfAbsent(new ReviewHelpfulVote(
                reviewId, userId, LocalDateTime.now(clock)));
        return new HelpfulResult(reviewId, helpfulPort.countByReviewId(reviewId), true);
    }

    @Override
    @Transactional
    public HelpfulResult unmarkHelpful(Long userId, Long reviewId) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requirePublicInteraction(userId);
        helpfulPort.delete(reviewId, userId);
        return new HelpfulResult(reviewId, helpfulPort.countByReviewId(reviewId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewReaction> listMyReviewReactions(Long userId, List<Long> reviewIds) {
        List<Long> normalizedIds = reviewIds.stream().distinct().toList();
        Set<Long> helpfulIds = Set.copyOf(
                helpfulPort.findHelpfulReviewIds(userId, normalizedIds));
        Set<Long> reportedIds = Set.copyOf(
                reportPort.findReportedReviewIds(userId, normalizedIds));
        Map<Long, ReviewInteractionStateView> states = reviewReader
                .findInteractionStates(normalizedIds).stream()
                .collect(Collectors.toMap(
                        ReviewInteractionStateView::reviewId, Function.identity()));
        return normalizedIds.stream()
                .map(reviewId -> {
                    ReviewInteractionStateView state = states.get(reviewId);
                    boolean ownedByMe = state != null && userId.equals(state.ownerUserId());
                    boolean canInteract = state != null
                            && state.status() == ReviewStatus.PUBLISHED
                            && !ownedByMe;
                    return new ReviewReaction(
                            reviewId,
                            helpfulIds.contains(reviewId),
                            reportedIds.contains(reviewId),
                            ownedByMe,
                            canInteract);
                })
                .toList();
    }

    @Override
    public ReviewImageItem addReviewImage(
            Long userId, Long reviewId, byte[] bytes, String contentType) {
        return toImageItem(imageUploadService.upload(
                userId, reviewId, bytes, contentType));
    }

    @Override
    public void deleteReviewImage(Long userId, Long reviewId, Long imageId) {
        imageAttachmentService.remove(userId, reviewId, imageId);
    }

    private void requireProductSourceAvailable(Long orderItemId) {
        eligibilityPort.findProductSourceReservation(orderItemId)
                .ifPresent(DefaultReviewService::throwReservationConflict);
    }

    private void requireClassSourceAvailable(Long bookingId) {
        eligibilityPort.findClassSourceReservation(bookingId)
                .ifPresent(DefaultReviewService::throwReservationConflict);
    }

    private static void throwReservationConflict(ReviewSourceReservationView reservation) {
        if (!reservation.active() && reservation.recreationBlocked()) {
            throw new HappyGalleryException(ErrorCode.REVIEW_RECREATION_BLOCKED);
        }
        throw new HappyGalleryException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    private Review ownedReviewForUpdate(Long userId, Long reviewId) {
        return reviewReader.findByIdAndUserIdForUpdate(reviewId, userId)
                .orElseThrow(NotFoundException.supplier("후기"));
    }

    private ReviewItem savedView(Review review, boolean includeWithdrawnUsers) {
        return reviewReader.findViewById(review.getId())
                .map(view -> toItems(List.of(view), includeWithdrawnUsers).getFirst())
                .orElseThrow(NotFoundException.supplier("후기"));
    }

    private PublicReviewPage publicPage(
            ReviewSummaryView summary,
            long filteredCount,
            List<ReviewListView> fetched,
            int pageSize,
            ReviewSort sort,
            Integer ratingFilter) {
        List<ReviewItem> items = toItems(fetched, false);
        CursorPage<ReviewItem> page = CursorPage.of(
                items,
                pageSize,
                item -> ReviewPublicCursor.encode(
                        sort, ratingFilter, item.rating(), item.createdAt(), item.id()));
        return new PublicReviewPage(toSummary(summary), filteredCount, page);
    }

    private CursorPage<ReviewItem> standardPage(
            List<ReviewListView> fetched, int pageSize, boolean includeWithdrawnUsers) {
        return CursorPage.of(
                toItems(fetched, includeWithdrawnUsers),
                pageSize,
                item -> CursorUtils.encode(item.createdAt(), item.id()));
    }

    private List<ReviewItem> toItems(
            List<ReviewListView> views, boolean includeWithdrawnUsers) {
        if (views.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = views.stream().map(ReviewListView::userId).distinct().toList();
        List<Long> reviewIds = views.stream().map(ReviewListView::id).toList();
        List<User> users = includeWithdrawnUsers
                ? userReader.findAllByIdForAdminHistory(userIds)
                : userReader.findAllById(userIds);
        Map<Long, String> authorNames = users.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        Map<Long, Long> helpfulCounts = helpfulPort.countByReviewIds(reviewIds).stream()
                .collect(Collectors.toMap(
                        ReviewHelpfulPort.ReviewHelpfulCountView::reviewId,
                        ReviewHelpfulPort.ReviewHelpfulCountView::helpfulCount));
        Map<Long, List<ReviewImageItem>> images = imagePort.findByReviewIds(reviewIds).stream()
                .collect(Collectors.groupingBy(
                        ReviewImage::getReviewId,
                        Collectors.mapping(DefaultReviewService::toImageItem, Collectors.toList())));
        return views.stream()
                .map(view -> toItem(
                        view,
                        authorNames.getOrDefault(view.userId(), "탈퇴회원"),
                        helpfulCounts.getOrDefault(view.id(), 0L),
                        images.getOrDefault(view.id(), List.of())))
                .toList();
    }

    private static ReviewItem toItem(
            ReviewListView view,
            String authorName,
            long helpfulCount,
            List<ReviewImageItem> images) {
        OfficialReplyItem reply = view.replyContent() == null
                ? null
                : new OfficialReplyItem(
                        view.replyContent(),
                        view.replyAdminId(),
                        view.replyCreatedAt(),
                        view.replyEditedAt());
        return new ReviewItem(
                view.id(),
                view.userId(),
                authorName,
                view.targetType(),
                view.sourceId(),
                view.targetId(),
                view.targetName(),
                view.rating(),
                view.content(),
                view.status(),
                view.contentRevision(),
                view.version(),
                view.hiddenReason(),
                view.hiddenAt(),
                view.hiddenByAdminId(),
                view.createdAt(),
                view.updatedAt(),
                view.editedAt(),
                view.editedAt() != null,
                true,
                reply,
                helpfulCount,
                images);
    }

    private static ReviewSummary toSummary(ReviewSummaryView view) {
        return new ReviewSummary(
                view.reviewCount(),
                view.averageRating(),
                new RatingHistogram(
                        view.rating1(),
                        view.rating2(),
                        view.rating3(),
                        view.rating4(),
                        view.rating5()));
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

    private static ReviewReportItem toReportItem(
            ReviewReport report, ReviewEvidenceSnapshot evidence) {
        return new ReviewReportItem(
                report.getId(),
                report.getReviewId(),
                report.getReporterUserId(),
                report.getReason(),
                report.getDetail(),
                report.getSnapshotStatus(),
                toEvidenceItem(evidence),
                report.getStatus(),
                report.getDecisionNote(),
                report.getDecidedByAdminId(),
                report.getDecidedAt(),
                report.getCreatedAt());
    }

    private static ReviewReportSummaryItem toReportSummaryItem(ReviewReportListView report) {
        return new ReviewReportSummaryItem(
                report.id(),
                report.reviewId(),
                report.reason(),
                report.snapshotStatus(),
                report.status(),
                report.createdAt());
    }

    private Map<Long, ReviewEvidenceSnapshot> evidenceById(List<Long> snapshotIds) {
        List<Long> normalized = snapshotIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return evidencePort.findByIds(normalized).stream()
                .collect(Collectors.toMap(ReviewEvidenceSnapshot::getId, Function.identity()));
    }

    private static ReviewEvidenceItem toEvidenceItem(ReviewEvidenceSnapshot evidence) {
        if (evidence == null) {
            return null;
        }
        return new ReviewEvidenceItem(
                evidence.getId(),
                evidence.getContentRevision(),
                evidence.getRating(),
                evidence.getContent(),
                evidence.getEditedAt(),
                evidence.getProvenance(),
                evidence.isImagesComplete(),
                evidence.getImageUrls(),
                evidence.getCapturedAt());
    }

    private static ReviewImageItem toImageItem(ReviewImage image) {
        return new ReviewImageItem(
                image.getId(), image.getImageUrl(), image.getSortOrder(), image.getCreatedAt());
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

    private static ReviewPublicCursor.CursorParam decodeCursor(
            String cursor, ReviewSort sort, Integer ratingFilter) {
        return cursor == null ? null : ReviewPublicCursor.decode(cursor, sort, ratingFilter);
    }
}
