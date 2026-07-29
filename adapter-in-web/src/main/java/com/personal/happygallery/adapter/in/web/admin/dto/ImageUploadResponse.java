package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.StoredImage;
import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String url
) {

    public static ImageUploadResponse from(StoredImage image) {
        return new ImageUploadResponse(image.url());
    }
}
