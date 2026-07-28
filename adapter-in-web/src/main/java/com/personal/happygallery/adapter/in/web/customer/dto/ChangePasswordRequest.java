package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 8, max = 72) @Utf8ByteLength(max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) @Utf8ByteLength(max = 72) String newPassword) {
}
