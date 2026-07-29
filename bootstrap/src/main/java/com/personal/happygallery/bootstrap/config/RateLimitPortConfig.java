package com.personal.happygallery.bootstrap.config;

import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.out.EmailVerificationRateLimitGuard;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RateLimitPortConfig {

    @Bean
    PhoneVerificationAttemptGuard phoneVerificationAttemptGuard(SubjectRateLimitGuard guard) {
        return guard::checkPhoneVerificationAttempt;
    }

    @Bean
    EmailVerificationRateLimitGuard emailVerificationRateLimitGuard(
            SubjectRateLimitGuard guard
    ) {
        return new EmailVerificationRateLimitGuard() {
            @Override
            public void checkIssue(Long userId, String normalizedEmail) {
                guard.checkEmailVerificationIssue(userId, normalizedEmail);
            }

            @Override
            public void checkAttempt(Long userId, String normalizedEmail) {
                guard.checkEmailVerificationAttempt(userId, normalizedEmail);
            }
        };
    }
}
