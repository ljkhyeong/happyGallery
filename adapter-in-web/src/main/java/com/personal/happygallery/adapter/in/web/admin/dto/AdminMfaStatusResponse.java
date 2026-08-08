package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.MfaStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminMfaStatusResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enabled,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean enrollmentPending,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        long recoveryCodesRemaining,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean recoveryResetAvailable
) {
    public static AdminMfaStatusResponse from(
            MfaStatus status,
            boolean recoveryResetAvailable) {
        return new AdminMfaStatusResponse(
                status.enabled(),
                status.enrollmentPending(),
                status.recoveryCodesRemaining(),
                recoveryResetAvailable);
    }
}
