package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordReauthenticationRequest(
        @NotBlank
        @Size(max = 72)
        @Utf8ByteLength(max = 72)
        String currentPassword
) {}
