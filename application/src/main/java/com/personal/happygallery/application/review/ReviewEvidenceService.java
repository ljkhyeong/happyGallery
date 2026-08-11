package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.out.ReviewEvidencePort;
import com.personal.happygallery.application.review.port.out.ReviewImagePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.review.Review;
import com.personal.happygallery.domain.review.ReviewEvidenceSnapshot;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 잠긴 후기의 본문·평점·사진을 하나의 불변 증거로 캡처한다. */
@Service
class ReviewEvidenceService {

    static final Period RETENTION = Period.ofYears(3);

    private final ReviewEvidencePort evidencePort;
    private final ReviewImagePort imagePort;
    private final Clock clock;

    ReviewEvidenceService(
            ReviewEvidencePort evidencePort, ReviewImagePort imagePort, Clock clock) {
        this.evidencePort = evidencePort;
        this.imagePort = imagePort;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ReviewEvidenceSnapshot captureForModeration(Review review) {
        LocalDateTime now = LocalDateTime.now(clock);
        return capture(review, now, now.plus(RETENTION));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    ReviewEvidenceSnapshot captureForPendingReport(Review review) {
        return capture(review, LocalDateTime.now(clock), null);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    void startResolvedReportRetention(Long snapshotId, LocalDateTime decidedAt) {
        ReviewEvidenceSnapshot snapshot = evidencePort.findById(snapshotId)
                .orElseThrow(NotFoundException.supplier("후기 증거"));
        snapshot.startRetention(decidedAt.plus(RETENTION));
        evidencePort.save(snapshot);
    }

    private ReviewEvidenceSnapshot capture(
            Review review, LocalDateTime capturedAt, LocalDateTime retentionUntil) {
        List<String> imageUrls = imagePort.findByReviewId(review.getId()).stream()
                .map(image -> image.getImageUrl())
                .toList();
        return evidencePort.save(new ReviewEvidenceSnapshot(
                review.getId(),
                review.getContentRevision(),
                review.getRating(),
                review.getContent(),
                review.getEditedAt(),
                imageUrls,
                capturedAt,
                retentionUntil));
    }
}
