package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.order.port.in.SmartStoreChannelOrderUseCase.ReconcileActionCommand;
import com.personal.happygallery.domain.order.SmartStoreOrderReconciliationOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReconcileSmartStoreOrderActionRequest(
        @NotNull SmartStoreOrderReconciliationOutcome outcome,
        @NotBlank @Size(max = 500) String note
) {
    public ReconcileActionCommand toCommand() {
        return new ReconcileActionCommand(outcome, note);
    }
}
