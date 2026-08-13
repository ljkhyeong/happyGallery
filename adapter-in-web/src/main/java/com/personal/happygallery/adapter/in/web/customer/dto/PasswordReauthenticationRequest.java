package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordReauthenticationRequest(
        @Schema(maxLength = 72)
        @NotBlank
        @Utf8ByteLength(max = 72)
        String currentPassword
) {}
