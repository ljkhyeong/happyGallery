package com.personal.happygallery.app.web.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,
        @NotBlank String verificationCode,
        @NotBlank String name,
        @NotEmpty @Valid List<OrderItemDto> items
) {
    public record OrderItemDto(
            @Positive Long productId,
            @Positive int qty
    ) {}
}
