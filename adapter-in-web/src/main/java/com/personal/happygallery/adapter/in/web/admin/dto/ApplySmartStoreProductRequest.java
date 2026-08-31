package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplySmartStoreProductRequest(
        @NotBlank String previewVersion
) {}
