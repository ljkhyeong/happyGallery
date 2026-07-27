package com.personal.happygallery.adapter.in.web.security.customer;

public final class CustomerSecurityRoutes {

    public static final String AUTH_API_PATTERN = "/api/v1/auth/**";
    public static final String MEMBER_API = "/api/v1/me";
    public static final String MEMBER_API_PATTERN = MEMBER_API + "/**";
    public static final String PAYMENT_API_PATTERN = "/api/v1/payments/**";
    public static final String GUEST_RECORD_API_PATTERN = "/api/v1/guest-records/**";
    public static final String CLIENT_MONITORING_API = "/api/v1/monitoring/client-events";

    public static final String SOCIAL_AUTHORIZATION_BASE_URI = "/api/v1/auth/social/authorization";
    public static final String SOCIAL_AUTHORIZATION_PROVIDER_PATH =
            SOCIAL_AUTHORIZATION_BASE_URI + "/{provider}";
    private static final String SOCIAL_CALLBACK_ROOT = "/api/v1/auth/social/callback";
    public static final String SOCIAL_CALLBACK_BASE_URI = SOCIAL_CALLBACK_ROOT + "/*";
    public static final String SOCIAL_CALLBACK_PROVIDER_PATH =
            SOCIAL_CALLBACK_ROOT + "/{provider}";

    private CustomerSecurityRoutes() {
    }
}
