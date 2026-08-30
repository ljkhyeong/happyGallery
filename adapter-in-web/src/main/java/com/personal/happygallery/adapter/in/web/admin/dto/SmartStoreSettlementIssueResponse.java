package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.SmartStoreSettlementEntry;
import com.personal.happygallery.domain.order.SmartStoreSettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SmartStoreSettlementIssueResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String entryKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productOrderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String settleType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String productName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long paySettleAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long totalPayCommissionAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Long sellingInterlockCommissionAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long benefitSettleAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long settleExpectAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SmartStoreSettlementStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleBasisDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleExpectDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate settleCompleteDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate payDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime fetchedAt
) {

    public static SmartStoreSettlementIssueResponse from(SmartStoreSettlementEntry entry) {
        return new SmartStoreSettlementIssueResponse(
                entry.getEntryKey(), entry.getProductOrderId(), entry.getOrderId(),
                entry.getProductOrderType(), entry.getSettleType(), entry.getProductName(),
                entry.getPaySettleAmount(), entry.getTotalPayCommissionAmount(),
                entry.getSellingInterlockCommissionAmount(), entry.getBenefitSettleAmount(),
                entry.getSettleExpectAmount(), entry.getReconciliationStatus(),
                entry.getReconciliationReason(), entry.getSettleBasisDate(),
                entry.getSettleExpectDate(), entry.getSettleCompleteDate(),
                entry.getPayDate(), entry.getFetchedAt());
    }
}
