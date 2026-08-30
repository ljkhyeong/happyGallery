package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.MfaEnrollment;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminMfaEnrollmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String secret,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
        String provisioningUri
) {
    public static AdminMfaEnrollmentResponse from(MfaEnrollment enrollment) {
        return new AdminMfaEnrollmentResponse(
                enrollment.secret(), enrollment.provisioningUri());
    }
}
