package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content
) {}
