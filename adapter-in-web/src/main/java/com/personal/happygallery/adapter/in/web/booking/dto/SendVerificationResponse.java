package com.personal.happygallery.adapter.in.web.booking.dto;

import com.personal.happygallery.domain.booking.PhoneVerification;
import io.swagger.v3.oas.annotations.media.Schema;

public record SendVerificationResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long verificationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String phone
) {
    public static SendVerificationResponse from(PhoneVerification pv) {
        return new SendVerificationResponse(pv.getId(), pv.getPhone());
    }
}
