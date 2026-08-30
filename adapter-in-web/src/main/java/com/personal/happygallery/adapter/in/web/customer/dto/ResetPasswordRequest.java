package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String verificationCode,
        @NotBlank @Size(min = 8, max = 72) @Utf8ByteLength(max = 72) String newPassword) {
}
