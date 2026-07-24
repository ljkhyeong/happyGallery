package com.personal.happygallery.application.policy;

import com.personal.happygallery.domain.policy.PolicyConsent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.policy-consent")
public record PolicyConsentProperties(String termsVersion, String privacyVersion) {

    public PolicyConsentProperties {
        if (!StringUtils.hasText(termsVersion) || !StringUtils.hasText(privacyVersion)) {
            throw new IllegalArgumentException("정책 동의 버전 설정은 비어 있을 수 없습니다.");
        }
        termsVersion = termsVersion.trim();
        privacyVersion = privacyVersion.trim();
        if (termsVersion.length() > PolicyConsent.MAX_POLICY_VERSION_LENGTH
                || privacyVersion.length() > PolicyConsent.MAX_POLICY_VERSION_LENGTH) {
            throw new IllegalArgumentException(
                    "정책 동의 버전 설정은 "
                            + PolicyConsent.MAX_POLICY_VERSION_LENGTH
                            + "자 이하여야 합니다.");
        }
    }
}
