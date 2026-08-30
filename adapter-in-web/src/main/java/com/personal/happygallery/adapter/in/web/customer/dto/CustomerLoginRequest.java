package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.validation.Utf8ByteLength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Utf8ByteLength(max = 72) String password) {}
