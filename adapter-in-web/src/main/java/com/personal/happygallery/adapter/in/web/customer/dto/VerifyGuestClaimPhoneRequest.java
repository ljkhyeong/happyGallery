package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyGuestClaimPhoneRequest(@NotBlank String verificationCode) {}
