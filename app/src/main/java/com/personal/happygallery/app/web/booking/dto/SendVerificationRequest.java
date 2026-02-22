package com.personal.happygallery.app.web.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendVerificationRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "유효하지 않은 전화번호 형식입니다.")
        String phone
) {}
