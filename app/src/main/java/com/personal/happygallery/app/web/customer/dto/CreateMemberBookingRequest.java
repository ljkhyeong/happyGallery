package com.personal.happygallery.app.web.customer.dto;

import com.personal.happygallery.domain.booking.DepositPaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreateMemberBookingRequest(
        @NotNull Long slotId,
        long depositAmount,
        DepositPaymentMethod paymentMethod,
        Long passId) {}
