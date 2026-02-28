package com.personal.happygallery.app.web.pass.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PurchasePassRequest(
        @NotNull Long guestId,
        /** 8회권 총 결제금액 (KRW). null이면 0으로 처리. */
        @PositiveOrZero Long totalPrice
) {}
