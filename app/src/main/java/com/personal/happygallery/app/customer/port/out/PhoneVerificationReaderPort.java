package com.personal.happygallery.app.customer.port.out;

import com.personal.happygallery.domain.booking.PhoneVerification;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 전화번호 인증 조회 포트.
 */
public interface PhoneVerificationReaderPort {

    /** 미소모(verified=false) + 만료 전 인증 코드 조회 */
    Optional<PhoneVerification> findValidVerification(String phone, String code, LocalDateTime now);

    /** 전화번호 기준 가장 최근 미소모 인증 코드 조회 (local dev/E2E 전용) */
    Optional<PhoneVerification> findLatestUnverifiedCode(String phone);
}
