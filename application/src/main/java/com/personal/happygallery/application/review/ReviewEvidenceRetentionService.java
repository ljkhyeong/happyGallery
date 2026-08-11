package com.personal.happygallery.application.review;

import com.personal.happygallery.application.media.ImageMediaReferenceRemovedEvent;
import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.application.review.port.out.ReviewModerationPort;
import com.personal.happygallery.application.review.port.out.ReviewReportPort;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewEvidenceRetentionService {

    private final ReviewEvidencePort evidencePort;
    private final ReviewModerationPort moderationPort;
    private final ReviewReportPort reportPort;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewEvidenceRetentionService(
            ReviewEvidencePort evidencePort,
            ReviewModerationPort moderationPort,
            ReviewReportPort reportPort,
            ApplicationEventPublisher eventPublisher) {
        this.evidencePort = evidencePort;
        this.moderationPort = moderationPort;
        this.reportPort = reportPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public int deleteExpiredBatch(LocalDateTime now, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("삭제 배치 크기는 1 이상이어야 합니다.");
        }
        LocalDateTime cutoff = now.minus(ReviewEvidenceService.RETENTION);
        var actions = moderationPort.findBefore(cutoff, limit);
        moderationPort.deleteAll(actions);
        int deleted = actions.size();

        if (deleted < limit) {
            var reports = reportPort.findResolvedBefore(cutoff, limit - deleted);
            reportPort.deleteAll(reports);
            deleted += reports.size();
        }
        if (deleted < limit) {
            var expired = evidencePort.findExpired(now, limit - deleted);
            var evidence = evidencePort.findByIds(expired.stream()
                    .map(snapshot -> snapshot.getId())
                    .toList());
            List<String> removedImageUrls = evidence.stream()
                    .flatMap(snapshot -> snapshot.getImageUrls().stream())
                    .distinct()
                    .toList();
            evidencePort.deleteAll(evidence);
            if (!removedImageUrls.isEmpty()) {
                eventPublisher.publishEvent(
                        new ImageMediaReferenceRemovedEvent(removedImageUrls));
            }
            deleted += evidence.size();
        }
        return deleted;
    }
}
