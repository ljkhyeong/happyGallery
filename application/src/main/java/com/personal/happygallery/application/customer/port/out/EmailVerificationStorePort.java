package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.EmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationStorePort {

    EmailVerification save(EmailVerification verification);

    Optional<EmailVerification> findByIdForUpdate(
            Long verificationId,
            Long userId,
            long credentialVersion,
            String email);

    void invalidateEarlierUnconsumed(Long userId, Long verificationId);

    int deleteExpiredBefore(LocalDateTime cutoff, int limit);
}
