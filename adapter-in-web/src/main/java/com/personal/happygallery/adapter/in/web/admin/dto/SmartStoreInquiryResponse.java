package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.InquiryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreInquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long questionId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long channelProductId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maskedWriterId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String question,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String answer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean answered,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt
) {
    public static SmartStoreInquiryResponse from(InquiryResult result) {
        return new SmartStoreInquiryResponse(
                result.questionId(), result.channelProductId(), result.productName(),
                result.maskedWriterId(), result.question(), result.answer(), result.answered(),
                result.createdAt());
    }
}
