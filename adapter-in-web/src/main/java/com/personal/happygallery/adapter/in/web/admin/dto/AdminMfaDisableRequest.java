package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMfaDisableRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 72) @Utf8ByteLength(max = 72) String currentPassword,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 32) String code
) {}
