package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.domain.order.OrderClaim;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResolveOrderClaimRequest(
        @NotNull Boolean approved,
        @Positive Long refundAmount,
        @NotNull Boolean restoreInventory,
        @Size(max = OrderClaim.MAX_ADMIN_NOTE_LENGTH) String note
) {
    public AdminOrderClaimUseCase.ResolveCommand toCommand() {
        return new AdminOrderClaimUseCase.ResolveCommand(
                approved.booleanValue(), refundAmount, restoreInventory.booleanValue(), note);
    }
}
