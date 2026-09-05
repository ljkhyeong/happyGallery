package com.personal.happygallery.adapter.in.web.inquiry.dto;

import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record GroupInquiryPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<GroupInquirySummaryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore) {
    public static GroupInquiryPageResponse from(CursorPage<GroupInquiryUseCase.View> page) {
        return new GroupInquiryPageResponse(page.content().stream().map(GroupInquirySummaryResponse::from).toList(),
                page.nextCursor(), page.hasMore());
    }
}
