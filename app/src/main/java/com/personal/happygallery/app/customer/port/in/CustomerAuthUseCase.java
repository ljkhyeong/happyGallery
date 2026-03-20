package com.personal.happygallery.app.customer.port.in;

import com.personal.happygallery.domain.user.User;
import java.util.Optional;

/**
 * 고객 인증 유스케이스.
 *
 * <p>회원가입, 로그인, 사용자 조회를 포함한다.
 * 세션 저장/만료는 Spring Session이 담당하므로 이 인터페이스에서 제외된다.
 */
public interface CustomerAuthUseCase {

    record SignupCommand(String email, String rawPassword, String name, String phone) {}

    record LoginCommand(String email, String rawPassword) {}

    User signup(SignupCommand command);

    User login(LoginCommand command);

    Optional<User> findUser(Long userId);
}
