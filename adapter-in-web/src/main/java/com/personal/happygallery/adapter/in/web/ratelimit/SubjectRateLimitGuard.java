package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import java.util.Optional;
import org.springframework.stereotype.Component;

import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_CLOSED;
import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_OPEN;

@Component
public class SubjectRateLimitGuard {

    private final RateLimitProperties properties;
    private final RedisRateLimiter rateLimiter;

    public SubjectRateLimitGuard(RateLimitProperties properties, RedisRateLimiter rateLimiter) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    public void checkPhoneVerification(String phone) {
        check("PHONE_VERIFICATION_PHONE", phone,
                properties.subject().phoneVerification(), FAIL_CLOSED);
    }

    public void checkPhoneVerificationAttempt(String phone) {
        check("PHONE_VERIFICATION_ATTEMPT_PHONE", phone,
                properties.subject().phoneVerificationAttempt(), FAIL_CLOSED);
    }

    public void checkPaymentConfirm(String orderId) {
        check("PAYMENT_CONFIRM_ORDER", orderId,
                properties.subject().paymentConfirm(), FAIL_OPEN);
    }

    public void checkGuestClaim(long userId) {
        check("GUEST_CLAIM_USER", String.valueOf(userId),
                properties.subject().guestClaimVerify(), FAIL_CLOSED);
    }

    public void checkGuestRecordRecovery(String phone) {
        check("GUEST_RECORD_RECOVERY_PHONE", phone,
                properties.subject().guestRecordRecovery(), FAIL_CLOSED);
    }

    public void checkPassRefund(long userId) {
        check("PASS_REFUND_USER", String.valueOf(userId),
                properties.subject().passRefund(), FAIL_CLOSED);
    }

    private void check(String ruleId, String subject, Rule rule, RateLimitFailureMode failureMode) {
        if (!properties.enabled()) {
            return;
        }

        Optional<RateLimitDecision> result = rateLimiter.tryConsume(ruleId, subject, rule);
        if (result.isEmpty()) {
            if (failureMode == FAIL_CLOSED) {
                throw new RateLimitUnavailableException();
            }
            return;
        }

        RateLimitDecision decision = result.get();
        if (!decision.rejected()) {
            return;
        }

        throw new RateLimitExceededException(decision);
    }
}
