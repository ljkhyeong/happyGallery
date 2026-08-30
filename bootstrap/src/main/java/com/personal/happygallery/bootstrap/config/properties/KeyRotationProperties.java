package com.personal.happygallery.bootstrap.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.key-rotation")
public record KeyRotationProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String sourceKeyId
) {}
