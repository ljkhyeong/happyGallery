package com.personal.happygallery.bootstrap.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.external.google-oauth")
public record GoogleOAuthAppProperties(
        @DefaultValue("") String clientId
) {}
