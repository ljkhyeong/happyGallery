package com.personal.happygallery.adapter.in.web.order.dto;

import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import com.personal.happygallery.domain.order.OrderClaim;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderClaimRequest(
        @NotNull OrderClaimType type,
        @NotNull OrderClaimResolution requestedResolution,
        @NotBlank @Size(max = OrderClaim.MAX_REASON_LENGTH) String reason,
        @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid ClaimItemRequest> items
) {
    public OrderClaimUseCase.RequestCommand toCommand() {
        return new OrderClaimUseCase.RequestCommand(
                type,
                requestedResolution,
                reason,
                items.stream()
                        .map(item -> new OrderClaimUseCase.Item(item.orderItemId(), item.quantity()))
                        .toList());
    }

    public record ClaimItemRequest(
            @NotNull Long orderItemId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) @Positive int quantity
    ) {}
}
