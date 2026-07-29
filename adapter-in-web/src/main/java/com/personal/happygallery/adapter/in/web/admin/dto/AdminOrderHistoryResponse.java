package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.domain.order.OrderApprovalDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record AdminOrderHistoryResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OrderApprovalDecision decision,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long decidedByAdminId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDateTime decidedAt
) {

    public static AdminOrderHistoryResponse from(OrderHistoryResponse response) {
        return new AdminOrderHistoryResponse(
                response.id(),
                response.decision(),
                response.decidedByAdminId(),
                response.reason(),
                response.decidedAt());
    }
}
