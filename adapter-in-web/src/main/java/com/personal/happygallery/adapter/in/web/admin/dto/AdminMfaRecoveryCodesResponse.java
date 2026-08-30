package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase.RecoveryCodes;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record AdminMfaRecoveryCodesResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> recoveryCodes
) {
    public AdminMfaRecoveryCodesResponse {
        recoveryCodes = List.copyOf(recoveryCodes);
    }

    public static AdminMfaRecoveryCodesResponse from(RecoveryCodes result) {
        return new AdminMfaRecoveryCodesResponse(result.recoveryCodes());
    }
}
