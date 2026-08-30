package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.user.EmailAddress;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendEmailVerificationRequest(
        @NotBlank
        @Email
        @Size(max = EmailAddress.MAX_LENGTH)
        String email
) {}
