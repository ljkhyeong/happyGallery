package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ActionHistoryResult;
import com.personal.happygallery.domain.order.SmartStoreOrderAction;
import com.personal.happygallery.domain.order.SmartStoreOrderActionStatus;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record SmartStoreOrderActionHistoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String productOrderId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SmartStoreOrderAction action,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SmartStoreOrderActionStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String requestSummary,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String resultCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String resultMessage,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long changedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String changedBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime requestedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime completedAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        SmartStoreOrderReconciliationOutcome reconciliationOutcome,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reconciliationNote,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long reconciledByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reconciledBy,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDateTime reconciledAt
) {
    public static SmartStoreOrderActionHistoryResponse from(ActionHistoryResult result) {
        return new SmartStoreOrderActionHistoryResponse(
                result.id(), result.productOrderId(), result.action(), result.status(), result.requestSummary(),
                result.resultCode(), result.resultMessage(), result.changedByAdminId(),
                result.changedBy(), result.requestedAt(), result.completedAt(),
                result.reconciliationOutcome(), result.reconciliationNote(),
                result.reconciledByAdminId(), result.reconciledBy(), result.reconciledAt());
    }
}
