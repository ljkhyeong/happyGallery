package com.personal.happygallery.adapter.in.web.inquiry.dto;

import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record GroupInquirySummaryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiry.Source source,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiryStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String organization,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int headcount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String preferredSchedule,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String location,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String classInterest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt) {
    public static GroupInquirySummaryResponse from(GroupInquiryUseCase.View view) {
        var inquiry = view.inquiry();
        var details = view.details();
        return new GroupInquirySummaryResponse(inquiry.getId(), inquiry.getSource(), inquiry.getStatus(),
                details.organization(), details.headcount(), details.preferredSchedule(), details.location(),
                details.classInterest(), inquiry.getCreatedAt());
    }
}
