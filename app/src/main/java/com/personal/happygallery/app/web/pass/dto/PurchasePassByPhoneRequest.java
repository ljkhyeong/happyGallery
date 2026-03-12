package com.personal.happygallery.app.web.pass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record PurchasePassByPhoneRequest(
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,
        @NotBlank String verificationCode,
        @NotBlank String name,
        /** 8회권 총 결제금액 (KRW). null이면 0으로 처리. */
        @PositiveOrZero Long totalPrice
) {}
