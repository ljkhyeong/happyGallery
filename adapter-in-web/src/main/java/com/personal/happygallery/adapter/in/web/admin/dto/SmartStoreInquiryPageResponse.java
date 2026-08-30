package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.InquiryResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreInquiryPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SmartStoreInquiryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages
) {
    public static SmartStoreInquiryPageResponse from(OffsetPage<InquiryResult> result) {
        return new SmartStoreInquiryPageResponse(result.content().stream().map(SmartStoreInquiryResponse::from).toList(),
                result.page(), result.size(), result.totalCount(), result.totalPages());
    }
}
