package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreateMemberBookingRequest(
        @NotNull Long slotId,
        Long depositAmount,
        DepositPaymentMethod paymentMethod,
        Long passId) {}
