package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password) {}
