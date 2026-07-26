package com.personal.happygallery.adapter.in.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordChangeRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100) String currentPassword,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 10, max = 100) String newPassword
) {}
