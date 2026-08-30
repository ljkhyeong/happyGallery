package com.personal.happygallery.application.review;

import com.personal.happygallery.application.notification.ReviewNotificationPublisher;
import com.personal.happygallery.application.review.port.in.AdminReviewUseCase;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ModerationActionItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewEvidenceItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportItem;
import com.personal.happygallery.application.review.port.in.ReviewUseCase.ReviewReportSummaryItem;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.application.review.port.out.ReviewListView;
import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.application.review.port.out.ReviewReaderPort;
import com.personal.happygallery.application.review.port.out.ReviewReportListView;
import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import com.personal.happygallery.application.review.port.out.ReviewStorePort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import com.personal.happygallery.domain.review.ReviewModerationAction;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DefaultAdminReviewService implements AdminReviewUseCase {

    private final ReviewReaderPort reviewReader;
    private final ReviewStorePort reviewStore;
    private final ReviewEvidencePort evidencePort;
    private final ReviewModerationPort moderationPort;
    private final ReviewReportPort reportPort;
    private final ReviewEvidenceService evidenceService;
    private final ReviewNotificationPublisher notificationPublisher;
    private final ReviewViewAssembler viewAssembler;
    private final Clock clock;

    DefaultAdminReviewService(ReviewReaderPort reviewReader,
                              ReviewStorePort reviewStore,
                              ReviewEvidencePort evidencePort,
                              ReviewModerationPort moderationPort,
                              ReviewReportPort reportPort,
                              ReviewEvidenceService evidenceService,
                              ReviewNotificationPublisher notificationPublisher,
                              ReviewViewAssembler viewAssembler,
                              Clock clock) {
        this.reviewReader = reviewReader;
        this.reviewStore = reviewStore;
        this.evidencePort = evidencePort;
        this.moderationPort = moderationPort;
        this.reportPort = reportPort;
        this.evidenceService = evidenceService;
        this.notificationPublisher = notificationPublisher;
        this.viewAssembler = viewAssembler;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<ReviewItem> listAdminReviews(
            ReviewTargetType targetType, ReviewStatus status, String cursor, int size) {
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
        return viewAssembler.standardPage(fetched, pageSize, true);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewItem getAdminReview(Long reviewId) {
        ReviewListView view = reviewReader.findViewById(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        return viewAssembler.toItems(List.of(view), true).getFirst();
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
            return viewAssembler.savedView(review, true);
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
        return viewAssembler.savedView(review, true);
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
        return viewAssembler.savedView(review, true);
    }

    @Override
    @Transactional
    public ReviewItem deleteOfficialReply(Long reviewId, long expectedVersion) {
        Review review = reviewReader.findByIdForUpdate(reviewId)
                .orElseThrow(NotFoundException.supplier("후기"));
        review.requireVersion(expectedVersion);
        review.removeOfficialReply(LocalDateTime.now(clock));
        reviewStore.save(review);
        return viewAssembler.savedView(review, true);
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
                .map(DefaultAdminReviewService::toReportSummaryItem)
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

    private Map<Long, ReviewEvidenceSnapshot> evidenceById(List<Long> snapshotIds) {
        List<Long> normalized = snapshotIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return evidencePort.findByIds(normalized).stream()
                .collect(Collectors.toMap(ReviewEvidenceSnapshot::getId, Function.identity()));
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
}
