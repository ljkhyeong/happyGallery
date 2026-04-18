package com.personal.happygallery.application.customer.port.in;

import java.util.Optional;

/** local/E2E 전용 — 최근 미소모 인증 코드 조회. */
public interface DevPhoneVerificationQueryUseCase {

    Optional<String> findLatestUnverifiedCode(String phone);
}
