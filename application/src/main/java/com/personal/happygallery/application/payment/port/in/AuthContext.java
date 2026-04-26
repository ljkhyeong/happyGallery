package com.personal.happygallery.application.payment.port.in;

/**
 * prepare/confirm 호출 시점의 인증 정보.
 *
 * <p>회원 경로면 {@link #userId()}가 채워지고, 비회원(휴대폰 인증) 경로면 null이다.
 * payload 내부 userId/phone과 일치하는지 fulfiller 단계에서 교차 검증한다.
 */
public record AuthContext(Long userId) {

    public static AuthContext guest() {
        return new AuthContext(null);
    }

    public static AuthContext member(Long userId) {
        return new AuthContext(userId);
    }

    public boolean isMember() {
        return userId != null;
    }
}
