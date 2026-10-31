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

    // 보안 체인의 permitAll·CSRF 예외 목록과 처리율 제한 필터가 함께 쓰는 공개 변경 경로.
    public static final String LOGIN_PATH = "/api/v1/auth/login";
    public static final String SIGNUP_PATH = "/api/v1/auth/signup";
    public static final String PASSWORD_RESET_PATH = "/api/v1/auth/password/reset";
    public static final String PAYMENT_PREPARE_PATH = "/api/v1/payments/prepare";
    public static final String PAYMENT_CONFIRM_PATH = "/api/v1/payments/confirm";
    public static final String GUEST_RECORD_RECOVERY_PATH = "/api/v1/guest-records/recovery";
    public static final String GUEST_RECORD_PAYMENT_STATUS_RECOVERY_PATH =
            "/api/v1/guest-records/payment-status-recovery";
    public static final String PHONE_VERIFICATION_PATH = "/api/v1/bookings/phone-verifications";
    public static final String GROUP_INQUIRY_PATH = "/api/v1/group-inquiries";
    public static final String ORDER_PATH = "/api/v1/orders/{id}";
    public static final String ORDER_DELAY_RESPONSE_PATH = ORDER_PATH + "/delay-response";
    public static final String ORDER_SHIPPING_ADDRESS_PATH = ORDER_PATH + "/shipping-address";
    public static final String DELIVERY_TRACKING_WEBHOOK_PATH = "/api/v1/webhooks/delivery-tracking";
    public static final String TOSS_PAYMENTS_WEBHOOK_PATH = "/api/v1/webhooks/toss-payments";

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
