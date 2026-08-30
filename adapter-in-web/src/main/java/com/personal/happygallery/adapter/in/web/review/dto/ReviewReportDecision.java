package com.personal.happygallery.adapter.in.web.review.dto;

import com.personal.happygallery.domain.review.ReviewReportStatus;

public enum ReviewReportDecision {
    ACCEPTED,
    REJECTED;

    public ReviewReportStatus toStatus() {
        return ReviewReportStatus.valueOf(name());
    }
}
