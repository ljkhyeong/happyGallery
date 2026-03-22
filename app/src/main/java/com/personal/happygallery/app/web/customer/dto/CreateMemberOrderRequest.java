package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateMemberOrderRequest(
        @NotEmpty List<OrderItemDto> items) {
    public CreateMemberOrderRequest {
        items = items == null ? null : List.copyOf(items);
    }

    public record OrderItemDto(
            @NotNull Long productId,
            @Min(1) int qty) {}
}
