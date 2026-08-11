package com.personal.happygallery.application.review;

import com.personal.happygallery.application.review.port.out.ReviewTombstoneRetentionPort;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewTombstoneRetentionService {

    public static final Duration RETENTION = Duration.ofDays(30);

    private final ReviewTombstoneRetentionPort retentionPort;

    public ReviewTombstoneRetentionService(ReviewTombstoneRetentionPort retentionPort) {
        this.retentionPort = retentionPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatchBefore(LocalDateTime cutoff, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("삭제 배치 크기는 1 이상이어야 합니다.");
        }
        return retentionPort.deleteUnblockedBefore(cutoff, limit);
    }
}
