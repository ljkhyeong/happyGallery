package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.domain.booking.PhoneVerification;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 미소모(verified=false) + 만료 전 인증 코드 조회.
     * 조건: phone + code 일치 & verified=false & expiresAt > now
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhoneVerification> findByPhoneHmacAndCodeHmacAndDeliveredTrueAndVerifiedFalseAndExpiresAtAfter(
            String phoneHmac, String codeHmac, LocalDateTime now);

    /** 전화번호 기준 가장 최근 발송 성공·미소모 인증 코드 조회 (local dev/E2E 전용). */
    Optional<PhoneVerification> findTopByPhoneHmacAndDeliveredTrueAndVerifiedFalseOrderByIdDesc(String phoneHmac);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhoneVerification> findByIdAndPhoneHmac(Long id, String phoneHmac);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PhoneVerification verification
               set verification.verified = true
             where verification.phoneHmac = :phoneHmac
               and verification.id < :verificationId
               and verification.verified = false
            """)
    void invalidateEarlierUnconsumedForPhone(@Param("phoneHmac") String phoneHmac,
                                             @Param("verificationId") Long verificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM phone_verifications
            WHERE expires_at <= :cutoff
            ORDER BY expires_at, id
            LIMIT :limit
            """, nativeQuery = true)
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff,
                            @Param("limit") int limit);
}
