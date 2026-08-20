package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.domain.user.EmailAddress;
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

    public void checkCustomerLogin(String email) {
        check("CUSTOMER_LOGIN_EMAIL", EmailAddress.required(email),
                properties.subject().customerLogin(), FAIL_CLOSED);
    }

    public void checkCustomerReauthentication(long userId) {
        check("CUSTOMER_REAUTHENTICATION_USER", String.valueOf(userId),
                properties.subject().customerLogin(), FAIL_CLOSED);
    }

    public void checkPhoneVerification(String phone) {
        check("PHONE_VERIFICATION_PHONE", phone,
                properties.subject().phoneVerification(), FAIL_CLOSED);
    }

    public void checkPhoneVerificationAttempt(String normalizedPhone) {
        check("PHONE_VERIFICATION_ATTEMPT_PHONE", normalizedPhone,
                properties.subject().phoneVerificationAttempt(), FAIL_CLOSED);
    }

    public void checkEmailVerificationIssue(long userId, String normalizedEmail) {
        check("EMAIL_VERIFICATION_USER", String.valueOf(userId),
                properties.subject().emailVerification(), FAIL_CLOSED);
        check("EMAIL_VERIFICATION_EMAIL", normalizedEmail,
                properties.subject().emailVerification(), FAIL_CLOSED);
    }

    public void checkEmailVerificationAttempt(long userId, String normalizedEmail) {
        check("EMAIL_VERIFICATION_ATTEMPT_USER", String.valueOf(userId),
                properties.subject().emailVerificationAttempt(), FAIL_CLOSED);
        check("EMAIL_VERIFICATION_ATTEMPT_EMAIL", normalizedEmail,
                properties.subject().emailVerificationAttempt(), FAIL_CLOSED);
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

    public void checkReviewMutation(long userId) {
        check("REVIEW_MUTATION_USER", String.valueOf(userId),
                properties.subject().reviewMutation(), FAIL_CLOSED);
    }

    public void checkReviewHelpful(long userId) {
        check("REVIEW_HELPFUL_USER", String.valueOf(userId),
                properties.subject().reviewHelpful(), FAIL_CLOSED);
    }

    public void checkReviewReport(long userId) {
        check("REVIEW_REPORT_USER", String.valueOf(userId),
                properties.subject().reviewReport(), FAIL_CLOSED);
    }

    public void checkReviewImageUpload(long userId) {
        check("REVIEW_IMAGE_UPLOAD_USER", String.valueOf(userId),
                properties.subject().reviewImageUpload(), FAIL_CLOSED);
    }

    public void checkAdminMfaRecovery(long adminUserId) {
        check("ADMIN_MFA_RECOVERY_USER", String.valueOf(adminUserId),
                properties.subject().adminMfaRecovery(), FAIL_CLOSED);
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
