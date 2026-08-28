package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.in.ReviewInteractionUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.HelpfulResult;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewEvidenceItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReaction;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.application.review.port.out.ReviewHelpfulPort;
import com.personal.happygallery.application.review.port.out.ReviewInteractionStateView;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import com.personal.happygallery.domain.review.ReviewHelpfulVote;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultReviewInteractionService implements ReviewInteractionUseCase {

    private final ReviewReaderPort reviewReader;
    private final ReviewReportPort reportPort;
    private final ReviewHelpfulPort helpfulPort;
    private final ReviewEvidenceService evidenceService;
    private final Clock clock;

    DefaultReviewInteractionService(ReviewReaderPort reviewReader,
                                    ReviewReportPort reportPort,
                                    ReviewHelpfulPort helpfulPort,
                                    ReviewEvidenceService evidenceService,
                                    Clock clock) {
        this.reviewReader = reviewReader;
        this.reportPort = reportPort;
        this.helpfulPort = helpfulPort;
        this.evidenceService = evidenceService;
        this.clock = clock;
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

    private static ReviewEvidenceItem toEvidenceItem(ReviewEvidenceSnapshot evidence) {
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
}
