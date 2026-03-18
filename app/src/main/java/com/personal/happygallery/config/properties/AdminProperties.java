package com.personal.happygallery.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @DefaultValue("") String apiKey,
        @DefaultValue("false") boolean enableApiKeyAuth
) {}
