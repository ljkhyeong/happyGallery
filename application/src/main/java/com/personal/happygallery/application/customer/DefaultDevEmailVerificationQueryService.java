package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.DevEmailVerificationQueryUseCase;
import com.personal.happygallery.application.customer.port.out.EmailVerificationReaderPort;
import com.personal.happygallery.domain.user.EmailVerification;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile({"local", "dev"})
@Service
public class DefaultDevEmailVerificationQueryService
        implements DevEmailVerificationQueryUseCase {

    private final EmailVerificationReaderPort verificationReader;

    public DefaultDevEmailVerificationQueryService(
            EmailVerificationReaderPort verificationReader
    ) {
        this.verificationReader = verificationReader;
    }

    @Override
    public Optional<String> findLatestUnverifiedCode(Long userId, String email) {
        return verificationReader.findLatestUnverifiedCode(userId, email)
                .map(EmailVerification::getCode);
    }
}
