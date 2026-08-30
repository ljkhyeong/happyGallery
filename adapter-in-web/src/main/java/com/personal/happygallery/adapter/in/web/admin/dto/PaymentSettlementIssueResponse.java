package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.payment.PaymentSettlement;
import com.personal.happygallery.domain.payment.PaymentSettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentSettlementIssueResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String transactionKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String paymentKey,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String orderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long amount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long payOutAmount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean cancelTransaction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PaymentSettlementStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate soldDate,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime fetchedAt
) {

    public static PaymentSettlementIssueResponse from(PaymentSettlement settlement) {
        return new PaymentSettlementIssueResponse(
                settlement.getId(),
                settlement.getTransactionKey(),
                settlement.getPaymentKey(),
                settlement.getOrderIdExternal(),
                settlement.getAmount(),
                settlement.getPayOutAmount(),
                settlement.isCancelTransaction(),
                settlement.getReconciliationStatus(),
                settlement.getReconciliationReason(),
                settlement.getSoldDate(),
                settlement.getFetchedAt());
    }
}
