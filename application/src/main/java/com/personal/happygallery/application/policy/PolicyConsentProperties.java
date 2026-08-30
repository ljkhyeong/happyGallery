package com.personal.happygallery.application.policy;

import com.personal.happygallery.domain.policy.PolicyConsent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.policy-consent")
public record PolicyConsentProperties(
        @NotBlank @Size(max = PolicyConsent.MAX_POLICY_VERSION_LENGTH) String termsVersion,
        @NotBlank @Size(max = PolicyConsent.MAX_POLICY_VERSION_LENGTH) String privacyVersion
) {

    public PolicyConsentProperties {
        termsVersion = termsVersion == null ? null : termsVersion.trim();
        privacyVersion = privacyVersion == null ? null : privacyVersion.trim();
    }
}
