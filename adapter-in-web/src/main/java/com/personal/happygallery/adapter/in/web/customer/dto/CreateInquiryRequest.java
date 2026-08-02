package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInquiryRequest(
        @NotBlank
        @Size(min = ContentTextPolicy.MIN_LENGTH, max = ContentTextPolicy.MAX_TITLE_LENGTH)
        String title,
        @NotBlank
        @Size(min = ContentTextPolicy.MIN_LENGTH, max = ContentTextPolicy.MAX_BODY_LENGTH)
        String content
) {}
