package com.personal.happygallery.app.customer.port.in;

import com.personal.happygallery.domain.user.User;
import java.util.Optional;

/**
 * 고객 인증 유스케이스.
 *
 * <p>회원가입, 로그인, 로그아웃, 세션 검증을 포함한다.
 */
public interface CustomerAuthUseCase {

    TokenResult signup(String email, String rawPassword, String name, String phone);

    TokenResult login(String email, String rawPassword);

    void logout(String rawToken);

    Optional<User> validateSession(String rawToken);

    record TokenResult(String rawToken, User user) {}
}
