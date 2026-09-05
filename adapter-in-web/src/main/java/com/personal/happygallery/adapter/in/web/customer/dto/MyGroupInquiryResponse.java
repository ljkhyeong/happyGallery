package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquirySummaryResponse;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record MyGroupInquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquirySummaryResponse summary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MyGroupInquiryChange> changes) {
    public record MyGroupInquiryChange(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String note,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt) {}

    public static MyGroupInquiryResponse from(GroupInquiryUseCase.MemberDetail detail) {
        return new MyGroupInquiryResponse(GroupInquirySummaryResponse.from(detail.view()), detail.view().inquiry().getVersion(),
                detail.changes().stream().map(change -> new MyGroupInquiryChange(change.activity().getId(),
                        change.note(), change.activity().getCreatedAt())).toList());
    }
}
