package com.personal.happygallery.application.customer.port.in;

import java.util.Optional;

/** local/E2E 전용 이메일 인증 코드 조회. */
public interface DevEmailVerificationQueryUseCase {

    Optional<String> findLatestUnverifiedCode(Long userId, String email);
}
