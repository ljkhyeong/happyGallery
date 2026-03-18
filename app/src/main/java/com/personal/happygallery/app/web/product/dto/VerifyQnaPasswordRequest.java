package com.personal.happygallery.app.web.product.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyQnaPasswordRequest(@NotBlank String password) {}
