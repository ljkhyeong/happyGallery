package com.personal.happygallery.adapter.in.web.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotNull @DefaultValue("") String apiKey,
        @DefaultValue("false") boolean enableApiKeyAuth
) {}
