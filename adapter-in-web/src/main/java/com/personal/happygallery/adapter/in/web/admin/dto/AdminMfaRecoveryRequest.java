package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMfaRecoveryRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        @NotBlank @Size(min = 1, max = 72) @Utf8ByteLength(max = 72) String currentPassword
) {}
