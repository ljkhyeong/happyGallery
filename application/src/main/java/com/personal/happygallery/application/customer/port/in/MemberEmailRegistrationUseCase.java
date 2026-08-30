package com.personal.happygallery.application.customer.port.in;

public interface MemberEmailRegistrationUseCase {

    void sendVerificationCode(SendVerificationCommand command);

    void registerVerifiedEmail(RegisterEmailCommand command);

    record SendVerificationCommand(
            Long userId,
            long credentialVersion,
            String email,
            boolean recentlyReauthenticated
    ) {}

    record RegisterEmailCommand(
            Long userId,
            long credentialVersion,
            String email,
            String verificationCode,
            boolean recentlyReauthenticated
    ) {}
}
