package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

public record GuestClaimResultResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int claimedOrderCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int claimedBookingCount
) {
    public static GuestClaimResultResponse from(GuestClaimUseCase.ClaimResult result) {
        return new GuestClaimResultResponse(
                result.claimedOrderCount(),
                result.claimedBookingCount());
    }
}
