package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.naver-oauth")
public record NaverOAuthProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("https://nid.naver.com/oauth2.0/token") String tokenUrl,
        @DefaultValue("https://openapi.naver.com/v1/nid/me") String userInfoUrl,
        @Min(1) @DefaultValue("5000") long timeoutMillis,
        @Min(1) @DefaultValue("2000") long connectTimeoutMillis,
        @Min(1) @DefaultValue("1000") long acquireTimeoutMillis,
        @Min(1) @DefaultValue("10") int maxConnections,
        @Min(1) @DefaultValue("30000") long keepAliveMillis
) implements HttpPoolProperties {}
