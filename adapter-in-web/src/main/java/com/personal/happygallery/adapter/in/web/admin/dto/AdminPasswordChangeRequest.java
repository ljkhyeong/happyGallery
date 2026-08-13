package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPasswordChangeRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 72)
        @NotBlank @Utf8ByteLength(max = 72) String currentPassword,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 10, max = 72) @Utf8ByteLength(max = 72) String newPassword
) {}
