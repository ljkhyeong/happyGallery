package com.personal.happygallery.bootstrap.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.field-encryption")
public record FieldEncryptionProperties(
        @NotBlank String encryptKey,
        @NotBlank String hmacKey
) {}
