package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.CustomerInquiryResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreCustomerInquiryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long inquiryNo,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long answerContentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String category,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String inquiryContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String answerContent,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean answered,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String channelProductId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productOrderIds,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productOrderOption,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String maskedCustomerId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String customerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime answeredAt
) {
    public static SmartStoreCustomerInquiryResponse from(CustomerInquiryResult result) {
        return new SmartStoreCustomerInquiryResponse(
                result.inquiryNo(), result.answerContentId(), result.category(), result.title(), result.inquiryContent(),
                result.answerContent(), result.answered(), result.orderId(),
                result.channelProductId(), result.productOrderIds(), result.productName(),
                result.productOrderOption(), result.maskedCustomerId(), result.customerName(),
                result.createdAt(), result.answeredAt());
    }
}
