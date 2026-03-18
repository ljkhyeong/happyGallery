package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQnaRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        boolean secret,
        @Size(min = 4, max = 20) String password
) {}
