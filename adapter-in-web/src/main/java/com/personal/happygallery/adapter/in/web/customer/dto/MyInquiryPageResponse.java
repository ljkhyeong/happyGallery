package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.inquiry.Inquiry;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyInquiryPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<InquiryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String nextCursor,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean hasMore
) {
    public static MyInquiryPageResponse from(CursorPage<Inquiry> page) {
        return new MyInquiryPageResponse(
                page.content().stream().map(InquiryResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }
}
