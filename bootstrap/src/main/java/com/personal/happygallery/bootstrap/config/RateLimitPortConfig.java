package com.personal.happygallery.bootstrap.config;

import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RateLimitPortConfig {

    @Bean
    PhoneVerificationAttemptGuard phoneVerificationAttemptGuard(SubjectRateLimitGuard guard) {
        return guard::checkPhoneVerificationAttempt;
    }
}
