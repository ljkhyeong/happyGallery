package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateMemberBookingRequest(
        @NotNull Long slotId,
        @Positive(message = "예약금은 0보다 커야 합니다.") Long depositAmount,
        DepositPaymentMethod paymentMethod,
        Long passId) {}
