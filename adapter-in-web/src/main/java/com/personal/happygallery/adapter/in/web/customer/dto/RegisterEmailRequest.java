package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.user.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterEmailRequest(
        @NotBlank
        @Email
        @Size(max = EmailAddress.MAX_LENGTH)
        String email,
        @NotBlank
        @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자여야 합니다.")
        String verificationCode
) {}
