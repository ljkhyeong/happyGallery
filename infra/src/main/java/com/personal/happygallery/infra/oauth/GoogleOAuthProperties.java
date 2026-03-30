package com.personal.happygallery.infra.oauth;

import com.personal.happygallery.infra.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Validated
@ConfigurationProperties(prefix = "app.external.google-oauth")
public record GoogleOAuthProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("https://oauth2.googleapis.com/token") String tokenUrl,
        @DefaultValue("https://www.googleapis.com/oauth2/v3/userinfo") String userInfoUrl,
        @Min(1) @DefaultValue("5000") long timeoutMillis,
        @Min(1) @DefaultValue("2000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("1000") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("10") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {}
