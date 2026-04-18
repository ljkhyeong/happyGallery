package com.personal.happygallery.adapter.in.web.product.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyQnaPasswordRequest(@NotBlank String password) {}
