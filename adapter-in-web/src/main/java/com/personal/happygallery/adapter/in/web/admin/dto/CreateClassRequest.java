package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateClassRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 30) String category,
        @Positive int durationMin,
        @Positive @Max(PaymentAmountPolicy.MAX_AMOUNT) long price,
        @PositiveOrZero int bufferMin
) {
}
