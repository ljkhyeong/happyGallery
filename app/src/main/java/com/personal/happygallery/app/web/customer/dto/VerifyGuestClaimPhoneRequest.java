package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyGuestClaimPhoneRequest(@NotBlank String verificationCode) {}
