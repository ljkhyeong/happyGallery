package com.personal.happygallery.adapter.in.web.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @NotBlank @DefaultValue("happygallery:rate") String keyPrefix,
        @Valid @NotNull IpRules ip,
        @Valid @NotNull SubjectRules subject
) {

    public record IpRules(
            @Valid @NotNull Rule defaultApi,
            @Valid @NotNull Rule phoneVerification,
            @Valid @NotNull Rule emailVerification,
            @Valid @NotNull Rule customerLogin,
            @Valid @NotNull Rule customerSignup,
            @Valid @NotNull Rule adminLogin,
            @Valid @NotNull Rule adminSetup,
            @Valid @NotNull Rule adminApi,
            @Valid @NotNull Rule socialLogin,
            @Valid @NotNull Rule paymentPrepare,
            @Valid @NotNull Rule paymentConfirm,
            @Valid @NotNull Rule guestClaimVerify,
            @Valid @NotNull Rule guestRecordRecovery,
            @Valid @NotNull Rule clientMonitoring,
            @Valid @NotNull Rule reviewReport,
            @Valid @NotNull Rule reviewImageUpload,
            @Valid @NotNull Rule orderCustomerAction
    ) {}

    public record SubjectRules(
            @Valid @NotNull Rule customerLogin,
            @Valid @NotNull Rule phoneVerification,
            @Valid @NotNull Rule phoneVerificationAttempt,
            @Valid @NotNull Rule emailVerification,
            @Valid @NotNull Rule emailVerificationAttempt,
            @Valid @NotNull Rule paymentConfirm,
            @Valid @NotNull Rule guestClaimVerify,
            @Valid @NotNull Rule guestRecordRecovery,
            @Valid @NotNull Rule passRefund,
            @Valid @NotNull Rule reviewMutation,
            @Valid @NotNull Rule reviewHelpful,
            @Valid @NotNull Rule reviewReport,
            @Valid @NotNull Rule reviewImageUpload,
            @Valid @NotNull Rule adminMfaRecovery
    ) {}

    public record Rule(
            @Min(1) long capacity,
            @NotNull @DurationMin(seconds = 1) Duration window
    ) {}
}
