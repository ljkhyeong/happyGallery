package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterInitialPhoneRequest(
        @NotBlank String phone,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String verificationCode) {
}
