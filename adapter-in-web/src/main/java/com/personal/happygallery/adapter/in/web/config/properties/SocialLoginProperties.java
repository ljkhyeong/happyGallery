package com.personal.happygallery.adapter.in.web.config.properties;

import com.personal.happygallery.domain.user.SocialProvider;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.social-login")
public record SocialLoginProperties(
        @NotBlank String googleRedirectUri,
        @NotBlank String naverRedirectUri
) {

    public SocialLoginProperties {
        validateCallbackUri(googleRedirectUri, SocialProvider.GOOGLE);
        validateCallbackUri(naverRedirectUri, SocialProvider.NAVER);
    }

    public String redirectUri(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> googleRedirectUri;
            case NAVER -> naverRedirectUri;
        };
    }

    private static void validateCallbackUri(String value, SocialProvider provider) {
        if (value == null || value.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(provider + " OAuth callback URI 형식이 올바르지 않습니다.", exception);
        }
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        String expectedPath = "/auth/callback/" + provider.name().toLowerCase(Locale.ROOT);
        if ((!secure && !localHttp)
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !expectedPath.equals(uri.getPath())) {
            throw new IllegalArgumentException(provider + " OAuth callback URI가 허용된 형식이 아닙니다.");
        }
    }
}
