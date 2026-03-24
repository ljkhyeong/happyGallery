package com.personal.happygallery.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.field-encryption")
public record FieldEncryptionProperties(
        String encryptKey,
        String hmacKey
) {}
