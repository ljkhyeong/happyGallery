package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.adapter.out.external.http.HttpPool;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 소셜 로그인 제공자별 HTTP 커넥션풀 설정. 키는 {@code app.external.<제공자>-oauth.*}를 그대로 쓴다. */
@Validated
@ConfigurationProperties(prefix = "app.external")
public record SocialOAuthHttpProperties(
        @Valid @DefaultValue HttpPool googleOauth,
        @Valid @DefaultValue HttpPool naverOauth,
        @Valid @DefaultValue HttpPool kakaoOauth
) {}
