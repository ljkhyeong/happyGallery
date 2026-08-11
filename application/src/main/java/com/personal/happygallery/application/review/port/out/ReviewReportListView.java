package com.personal.happygallery.application.review.port.out;

import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.domain.review.ReviewStatus;
import java.time.LocalDateTime;

/** 관리자 신고 목록에서 민감 상세와 증거를 제외한 조회 전용 projection이다. */
public record ReviewReportListView(
        Long id,
        Long reviewId,
        ReviewReportReason reason,
        ReviewStatus snapshotStatus,
        ReviewReportStatus status,
        LocalDateTime createdAt
) {}
