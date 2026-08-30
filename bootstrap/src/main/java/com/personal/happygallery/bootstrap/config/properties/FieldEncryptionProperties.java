package com.personal.happygallery.bootstrap.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.field-encryption")
public record FieldEncryptionProperties(
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$") @DefaultValue("v1") String activeKeyId,
        @NotBlank String encryptKey,
        @NotBlank String hmacKey,
        @DefaultValue("") String previousEncryptKeys,
        @DefaultValue("") String previousHmacKeys
) {}
