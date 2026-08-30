package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase.InquiryWithUser;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminInquiryPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<AdminInquiryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean hasMore
) {

    public static AdminInquiryPageResponse from(CursorPage<InquiryWithUser> page) {
        return new AdminInquiryPageResponse(
                page.content().stream().map(AdminInquiryResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
