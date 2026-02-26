package com.personal.happygallery.app.web.booking.dto;

import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record CreateGuestBookingRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "유효하지 않은 전화번호 형식입니다.")
        String phone,

        @NotBlank
        String verificationCode,

        @NotBlank
        String name,

        @NotNull
        Long slotId,

        @NotNull
        @Positive(message = "예약금은 0보다 커야 합니다.")
        Long depositAmount,

        /** 예약금 결제 수단. BANK_TRANSFER는 서비스 레이어에서 차단됨. */
        @NotNull
        DepositPaymentMethod paymentMethod
) {}
