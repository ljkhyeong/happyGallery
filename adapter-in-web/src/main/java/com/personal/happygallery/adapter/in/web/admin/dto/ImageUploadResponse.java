package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.StoredImage;

public record ImageUploadResponse(String url) {

    public static ImageUploadResponse from(StoredImage image) {
        return new ImageUploadResponse(image.url());
    }
}
