package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.CustomerInquiryResult;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record SmartStoreCustomerInquiryPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<SmartStoreCustomerInquiryResponse> content,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalPages
) {
    public static SmartStoreCustomerInquiryPageResponse from(OffsetPage<CustomerInquiryResult> result) {
        return new SmartStoreCustomerInquiryPageResponse(
                result.content().stream().map(SmartStoreCustomerInquiryResponse::from).toList(),
                result.page(), result.size(), result.totalCount(), result.totalPages());
    }
}
