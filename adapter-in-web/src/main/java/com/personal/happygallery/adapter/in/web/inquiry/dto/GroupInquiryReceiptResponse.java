package com.personal.happygallery.adapter.in.web.inquiry.dto;

import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record GroupInquiryReceiptResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) GroupInquiryStatus status) {
    public static GroupInquiryReceiptResponse from(GroupInquiryUseCase.View view) {
        return new GroupInquiryReceiptResponse(view.inquiry().getId(), view.inquiry().getStatus());
    }
}
