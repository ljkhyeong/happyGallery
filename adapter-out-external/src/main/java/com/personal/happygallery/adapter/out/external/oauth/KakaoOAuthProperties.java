package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.HttpPoolProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.external.kakao-oauth")
public record KakaoOAuthProperties(
        @NotNull @DurationMin(millis = 1) @DefaultValue("5s") Duration timeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DurationMin(millis = 1) @DefaultValue("1s") Duration acquireTimeout,
        @Min(1) @DefaultValue("10") int maxConnections,
        @NotNull @DurationMin(millis = 1) @DefaultValue("30s") Duration keepAlive
) implements HttpPoolProperties {}
