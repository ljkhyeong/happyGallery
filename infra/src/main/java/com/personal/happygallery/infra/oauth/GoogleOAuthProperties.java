package com.personal.happygallery.infra.oauth;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.external.google-oauth")
public record GoogleOAuthProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("https://oauth2.googleapis.com/token") String tokenUrl,
        @DefaultValue("https://www.googleapis.com/oauth2/v3/userinfo") String userInfoUrl,
        @Min(1) @DefaultValue("5000") long timeoutMillis
) {}
