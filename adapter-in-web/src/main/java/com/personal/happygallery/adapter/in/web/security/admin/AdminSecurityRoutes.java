package com.personal.happygallery.adapter.in.web.security.admin;

/**
 * 관리자 보안 체인과 처리율 제한 필터가 함께 참조하는 관리자 경로.
 *
 * <p>컨트롤러 매핑은 클래스 접두사 + 메서드 접미사 구조라 리터럴을 유지한다. 상수와 매핑이 같은 경로를
 * 가리키는지는 {@code SecurityBoundaryUseCaseIT}·{@code AdminLoginUseCaseIT}가 확인한다.
 */
public final class AdminSecurityRoutes {

    public static final String ADMIN_API = "/api/v1/admin";
    public static final String ADMIN_API_PATTERN = ADMIN_API + "/**";

    public static final String LOGIN_PATH = "/api/v1/admin/auth/login";
    public static final String LOGOUT_PATH = "/api/v1/admin/auth/logout";
    public static final String MFA_PATH = "/api/v1/admin/auth/mfa";
    public static final String MFA_VERIFY_PATH = MFA_PATH + "/verify";
    public static final String MFA_RECOVERY_PATH = MFA_PATH + "/recovery";
    public static final String MFA_ENROLLMENT_PATH = MFA_PATH + "/enrollment";
    public static final String MFA_ENROLLMENT_CONFIRM_PATH = MFA_ENROLLMENT_PATH + "/confirm";

    public static final String SETUP_PATH = "/api/v1/admin/setup";
    public static final String SETUP_STATUS_PATH = SETUP_PATH + "/status";

    private AdminSecurityRoutes() {
    }
}
