package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ClaimCouponRequest(
        @NotNull @Positive Long definitionId
) {}
