package com.personal.happygallery.adapter.out.persistence.user;

import com.personal.happygallery.domain.user.EmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Lock(PESSIMISTIC_WRITE)
    Optional<EmailVerification>
    findByUserIdAndCredentialVersionAndEmailHmacAndCodeHmacAndDeliveredTrueAndVerifiedFalseAndExpiresAtAfter(
            Long userId,
            long credentialVersion,
            String emailHmac,
            String codeHmac,
            LocalDateTime now);

    Optional<EmailVerification>
    findTopByUserIdAndEmailHmacAndDeliveredTrueAndVerifiedFalseOrderByIdDesc(
            Long userId,
            String emailHmac);

    @Lock(PESSIMISTIC_WRITE)
    Optional<EmailVerification>
    findByIdAndUserIdAndCredentialVersionAndEmailHmac(
            Long id,
            Long userId,
            long credentialVersion,
            String emailHmac);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EmailVerification verification
               set verification.verified = true
             where verification.userId = :userId
               and verification.id < :verificationId
               and verification.verified = false
            """)
    void invalidateEarlierUnconsumed(@Param("userId") Long userId,
                                     @Param("verificationId") Long verificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM email_verifications
            WHERE expires_at <= :cutoff
            ORDER BY expires_at, id
            LIMIT :limit
            """, nativeQuery = true)
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff,
                            @Param("limit") int limit);
}
