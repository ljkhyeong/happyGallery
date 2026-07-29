package com.personal.happygallery.adapter.out.persistence.media;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.media")
public record MediaStorageProperties(@NotBlank String storagePath) {}
