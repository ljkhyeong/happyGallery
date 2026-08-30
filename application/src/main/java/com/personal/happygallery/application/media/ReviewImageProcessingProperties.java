package com.personal.happygallery.application.media;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.media.review-image-processing")
public record ReviewImageProcessingProperties(
        @Min(1) int maxConcurrentDecodes) {
}
