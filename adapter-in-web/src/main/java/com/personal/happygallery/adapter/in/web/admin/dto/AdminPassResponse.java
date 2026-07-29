package com.personal.happygallery.adapter.in.web.admin.dto;

import static com.personal.happygallery.adapter.in.web.MaskingUtil.maskPhoneMiddle;

import com.personal.happygallery.application.search.dto.AdminPassStatus;
import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.domain.payment.RefundStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminPassResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long passId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String passNumber,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String customerName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String customerPhone,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AdminPassStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int remainingCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalCredits,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int futureBookingCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long expectedRefundAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) RefundStatus refundStatus
) {

    public static AdminPassResponse from(AdminPassView pass) {
        return new AdminPassResponse(
                pass.passId(),
                pass.passNumber(),
                pass.customerName(),
                maskPhoneMiddle(pass.customerPhone()),
                pass.status(),
                pass.remainingCredits(),
                pass.totalCredits(),
                pass.expiresAt(),
                pass.futureBookingCount(),
                pass.expectedRefundAmount(),
                pass.refundStatus());
    }

}
