package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Locale;
import org.springframework.web.util.UriComponentsBuilder;

public final class CustomerSecurityRoutes {

    public static final String AUTH_API_PATTERN = "/api/v1/auth/**";
    public static final String MEMBER_API = "/api/v1/me";
    public static final String MEMBER_API_PATTERN = MEMBER_API + "/**";
    public static final String PAYMENT_API_PATTERN = "/api/v1/payments/**";
    public static final String GUEST_RECORD_API_PATTERN = "/api/v1/guest-records/**";
    public static final String CLIENT_MONITORING_API = "/api/v1/monitoring/client-events";
    public static final String BOT_PROTECTION_API = "/api/v1/bot-protection";

    public static final String SOCIAL_AUTHORIZATION_BASE_URI = "/api/v1/auth/social/authorization";
    public static final String SOCIAL_AUTHORIZATION_PROVIDER_PATH =
            SOCIAL_AUTHORIZATION_BASE_URI + "/{provider}";
    public static final String SOCIAL_SIGNUP_INTENT_PROVIDER_PATH =
            "/api/v1/auth/social/signup-intents/{provider}";
    public static final String SOCIAL_SIGNUP_COMPLETION_PATH = "/api/v1/auth/social/signup-completion";
    private static final String SOCIAL_CALLBACK_ROOT = "/api/v1/auth/social/callback";
    public static final String SOCIAL_CALLBACK_BASE_URI = SOCIAL_CALLBACK_ROOT + "/*";
    public static final String SOCIAL_CALLBACK_PROVIDER_PATH =
            SOCIAL_CALLBACK_ROOT + "/{provider}";

    private CustomerSecurityRoutes() {
    }

    /** 소셜 제공자 인가 시작 경로에 연결·재인증·가입 시도 ID를 query parameter로 붙인다. */
    public static String socialAuthorizationUrl(
            SocialProvider provider, String attemptParameter, String attemptId) {
        return UriComponentsBuilder.fromPath(SOCIAL_AUTHORIZATION_BASE_URI)
                .pathSegment(provider.name().toLowerCase(Locale.ROOT))
                .queryParam(attemptParameter, attemptId)
                .build()
                .encode()
                .toUriString();
    }
}
