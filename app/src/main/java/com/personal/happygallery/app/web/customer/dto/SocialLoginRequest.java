package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginRequest(@NotBlank String code, @NotBlank String redirectUri) {}
