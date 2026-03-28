package com.personal.happygallery.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.guest-token")
public record GuestTokenProperties(
        @NotBlank String hmacSecret,
        @Min(1) @DefaultValue("168") long expiryHours
) {}
