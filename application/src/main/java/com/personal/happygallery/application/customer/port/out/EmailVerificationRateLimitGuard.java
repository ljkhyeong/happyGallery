package com.personal.happygallery.application.customer.port.out;

public interface EmailVerificationRateLimitGuard {

    void checkIssue(Long userId, String normalizedEmail);

    void checkAttempt(Long userId, String normalizedEmail);
}
