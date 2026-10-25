package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.IpRules;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.SubjectRules;
import java.time.Duration;

/**
 * 테스트용 {@link RateLimitProperties} 조립기.
 *
 * <p>모든 규칙은 100회/1분으로 시작하고 시나리오가 좁힐 규칙만 이름으로 지정한다. IP 규칙은 1분 창의 허용 횟수를,
 * 주체 규칙은 {@link Rule}을 받는다. {@code IpRules}·{@code SubjectRules}의 positional 생성자는 {@link #build()} 한 곳에서만 호출한다.
 */
public final class RateLimitPropertiesFixture {

    private static final String KEY_PREFIX = "test:rate";
    private static final Rule GENEROUS = perMinute(100);

    private Rule ipDefaultApi = GENEROUS;
    private Rule ipPhoneVerification = GENEROUS;
    private Rule ipEmailVerification = GENEROUS;
    private Rule ipCustomerLogin = GENEROUS;
    private Rule ipCustomerSignup = GENEROUS;
    private Rule ipAdminLogin = GENEROUS;
    private Rule ipAdminSetup = GENEROUS;
    private Rule ipAdminApi = GENEROUS;
    private Rule ipSocialLogin = GENEROUS;
    private Rule ipPaymentPrepare = GENEROUS;
    private Rule ipPaymentConfirm = GENEROUS;
    private Rule ipGuestClaimVerify = GENEROUS;
    private Rule ipGuestRecordRecovery = GENEROUS;
    private Rule ipClientMonitoring = GENEROUS;
    private Rule ipReviewReport = GENEROUS;
    private Rule ipReviewImageUpload = GENEROUS;
    private Rule ipOrderCustomerAction = GENEROUS;
    private Rule ipGroupInquiry = GENEROUS;

    private Rule subjectCustomerLogin = GENEROUS;
    private Rule subjectPhoneVerification = GENEROUS;
    private Rule subjectPhoneVerificationAttempt = GENEROUS;
    private Rule subjectEmailVerification = GENEROUS;
    private Rule subjectEmailVerificationAttempt = GENEROUS;
    private Rule subjectPaymentConfirm = GENEROUS;
    private Rule subjectGuestClaimVerify = GENEROUS;
    private Rule subjectGuestRecordRecovery = GENEROUS;
    private Rule subjectPassRefund = GENEROUS;
    private Rule subjectReviewMutation = GENEROUS;
    private Rule subjectReviewHelpful = GENEROUS;
    private Rule subjectReviewReport = GENEROUS;
    private Rule subjectReviewImageUpload = GENEROUS;
    private Rule subjectAdminMfaRecovery = GENEROUS;

    public RateLimitPropertiesFixture ipDefaultApi(long capacity) {
        return ipDefaultApi(capacity, Duration.ofMinutes(1));
    }

    public RateLimitPropertiesFixture ipDefaultApi(long capacity, Duration window) {
        ipDefaultApi = new Rule(capacity, window);
        return this;
    }

    public RateLimitPropertiesFixture ipPhoneVerification(long capacity) {
        ipPhoneVerification = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipEmailVerification(long capacity) {
        ipEmailVerification = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipCustomerLogin(long capacity) {
        ipCustomerLogin = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipCustomerSignup(long capacity) {
        ipCustomerSignup = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipAdminLogin(long capacity) {
        ipAdminLogin = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipAdminSetup(long capacity) {
        ipAdminSetup = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipAdminApi(long capacity) {
        ipAdminApi = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipSocialLogin(long capacity) {
        ipSocialLogin = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipPaymentPrepare(long capacity) {
        ipPaymentPrepare = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipPaymentConfirm(long capacity) {
        ipPaymentConfirm = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipGuestClaimVerify(long capacity) {
        ipGuestClaimVerify = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipGuestRecordRecovery(long capacity) {
        ipGuestRecordRecovery = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipClientMonitoring(long capacity) {
        ipClientMonitoring = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipReviewReport(long capacity) {
        ipReviewReport = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipReviewImageUpload(long capacity) {
        ipReviewImageUpload = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipOrderCustomerAction(long capacity) {
        ipOrderCustomerAction = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture ipGroupInquiry(long capacity) {
        ipGroupInquiry = perMinute(capacity);
        return this;
    }

    public RateLimitPropertiesFixture subjectCustomerLogin(Rule rule) {
        subjectCustomerLogin = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectPhoneVerification(Rule rule) {
        subjectPhoneVerification = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectPhoneVerificationAttempt(Rule rule) {
        subjectPhoneVerificationAttempt = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectEmailVerification(Rule rule) {
        subjectEmailVerification = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectEmailVerificationAttempt(Rule rule) {
        subjectEmailVerificationAttempt = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectPaymentConfirm(Rule rule) {
        subjectPaymentConfirm = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectGuestClaimVerify(Rule rule) {
        subjectGuestClaimVerify = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectGuestRecordRecovery(Rule rule) {
        subjectGuestRecordRecovery = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectPassRefund(Rule rule) {
        subjectPassRefund = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectReviewMutation(Rule rule) {
        subjectReviewMutation = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectReviewHelpful(Rule rule) {
        subjectReviewHelpful = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectReviewReport(Rule rule) {
        subjectReviewReport = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectReviewImageUpload(Rule rule) {
        subjectReviewImageUpload = rule;
        return this;
    }

    public RateLimitPropertiesFixture subjectAdminMfaRecovery(Rule rule) {
        subjectAdminMfaRecovery = rule;
        return this;
    }

    public RateLimitProperties build() {
        return new RateLimitProperties(
                true,
                KEY_PREFIX,
                new IpRules(
                        ipDefaultApi,
                        ipPhoneVerification,
                        ipEmailVerification,
                        ipCustomerLogin,
                        ipCustomerSignup,
                        ipAdminLogin,
                        ipAdminSetup,
                        ipAdminApi,
                        ipSocialLogin,
                        ipPaymentPrepare,
                        ipPaymentConfirm,
                        ipGuestClaimVerify,
                        ipGuestRecordRecovery,
                        ipClientMonitoring,
                        ipReviewReport,
                        ipReviewImageUpload,
                        ipOrderCustomerAction,
                        ipGroupInquiry),
                new SubjectRules(
                        subjectCustomerLogin,
                        subjectPhoneVerification,
                        subjectPhoneVerificationAttempt,
                        subjectEmailVerification,
                        subjectEmailVerificationAttempt,
                        subjectPaymentConfirm,
                        subjectGuestClaimVerify,
                        subjectGuestRecordRecovery,
                        subjectPassRefund,
                        subjectReviewMutation,
                        subjectReviewHelpful,
                        subjectReviewReport,
                        subjectReviewImageUpload,
                        subjectAdminMfaRecovery));
    }

    private static Rule perMinute(long capacity) {
        return new Rule(capacity, Duration.ofMinutes(1));
    }
}
