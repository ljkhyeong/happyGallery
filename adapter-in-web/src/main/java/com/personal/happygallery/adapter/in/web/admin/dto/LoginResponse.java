package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase.LoginResult;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase.LoginStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LoginStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String token,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        String challengeToken
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.status(), result.token(), result.challengeToken());
    }
}
