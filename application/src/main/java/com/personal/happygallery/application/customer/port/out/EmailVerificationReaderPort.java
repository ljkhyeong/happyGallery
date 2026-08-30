package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.EmailVerification;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationReaderPort {

    Optional<EmailVerification> findValidVerification(
            Long userId,
            long credentialVersion,
            String email,
            String code,
            LocalDateTime now);

    Optional<EmailVerification> findLatestUnverifiedCode(Long userId, String email);
}
