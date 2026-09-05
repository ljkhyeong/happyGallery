package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquirySummaryResponse;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

public record AdminGroupInquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquirySummaryResponse summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiryRequest details,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate nextContactOn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ActivityResponse> activities) {
    public record ActivityResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long adminId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean memberAction,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) GroupInquiryStatus fromStatus,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiryStatus toStatus,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String note,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt) {}

    public static AdminGroupInquiryResponse from(GroupInquiryUseCase.Detail detail) {
        return new AdminGroupInquiryResponse(GroupInquirySummaryResponse.from(detail.view()),
                GroupInquiryRequest.from(detail.view().details()), detail.view().inquiry().getVersion(), detail.view().inquiry().getNextContactOn(),
                detail.activities().stream().map(item -> new ActivityResponse(item.activity().getId(),
                        item.activity().getAdminId(), item.activity().isMemberAction(), item.activity().getFromStatus(), item.activity().getToStatus(),
                        item.note(), item.activity().getCreatedAt())).toList());
    }
}
