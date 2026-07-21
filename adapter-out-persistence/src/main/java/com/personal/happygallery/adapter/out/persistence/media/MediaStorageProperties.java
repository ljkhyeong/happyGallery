package com.personal.happygallery.adapter.out.persistence.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.media")
public record MediaStorageProperties(String storagePath) {

    public MediaStorageProperties {
        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("MEDIA_STORAGE_PATH는 비어 있을 수 없습니다.");
        }
    }
}
