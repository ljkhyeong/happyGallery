package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecoverGuestRecordsRequest(
        @NotBlank
        @Pattern(regexp = "^01[0-9]{8,9}$", message = "유효하지 않은 전화번호 형식입니다.")
        String phone,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String verificationCode
) {}
