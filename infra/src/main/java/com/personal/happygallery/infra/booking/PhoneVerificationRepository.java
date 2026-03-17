package com.personal.happygallery.infra.booking;

import com.personal.happygallery.domain.booking.PhoneVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 미소모(verified=false) + 만료 전 인증 코드 조회.
     * 조건: phone + code 일치 & verified=false & expiresAt > now
     */
    Optional<PhoneVerification> findByPhoneAndCodeAndVerifiedFalseAndExpiresAtAfter(
            String phone, String code, LocalDateTime now);

    /** 전화번호 기준 가장 최근 미소모 인증 코드 조회 (local dev/E2E 전용). */
    Optional<PhoneVerification> findTopByPhoneAndVerifiedFalseOrderByIdDesc(String phone);
}
