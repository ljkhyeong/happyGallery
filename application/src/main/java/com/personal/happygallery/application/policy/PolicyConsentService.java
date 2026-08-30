package com.personal.happygallery.application.policy;

import com.personal.happygallery.application.policy.port.out.PolicyConsentStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.policy.PolicyConsent;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.policy.PolicyConsentType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PolicyConsentService {

    private final PolicyConsentProperties properties;
    private final PolicyConsentStorePort consentStore;
    private final Clock clock;

    public PolicyConsentService(PolicyConsentProperties properties,
                                PolicyConsentStorePort consentStore,
                                Clock clock) {
        this.properties = properties;
        this.consentStore = consentStore;
        this.clock = clock;
    }

    public CurrentPolicy currentPolicy() {
        return new CurrentPolicy(properties.termsVersion(), properties.privacyVersion());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordForUser(Long userId,
                              PolicyConsentPurpose purpose,
                              PolicyAcceptance acceptance) {
        requireCurrent(acceptance);
        LocalDateTime acceptedAt = LocalDateTime.now(clock);
        consentStore.save(PolicyConsent.forUser(
                userId,
                PolicyConsentType.TERMS_OF_SERVICE,
                purpose,
                properties.termsVersion(),
                acceptedAt));
        consentStore.save(PolicyConsent.forUser(
                userId,
                PolicyConsentType.PRIVACY_POLICY,
                purpose,
                properties.privacyVersion(),
                acceptedAt));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordForPaymentAttempt(Long paymentAttemptId,
                                        PolicyConsentPurpose purpose,
                                        PolicyAcceptance acceptance) {
        requireCurrent(acceptance);
        LocalDateTime acceptedAt = LocalDateTime.now(clock);
        consentStore.save(PolicyConsent.forPaymentAttempt(
                paymentAttemptId,
                PolicyConsentType.TERMS_OF_SERVICE,
                purpose,
                properties.termsVersion(),
                acceptedAt));
        consentStore.save(PolicyConsent.forPaymentAttempt(
                paymentAttemptId,
                PolicyConsentType.PRIVACY_POLICY,
                purpose,
                properties.privacyVersion(),
                acceptedAt));
    }

    public void requireCurrent(PolicyAcceptance acceptance) {
        if (acceptance == null
                || !acceptance.termsAccepted()
                || !acceptance.privacyAccepted()
                || !Objects.equals(properties.termsVersion(), acceptance.termsVersion())
                || !Objects.equals(properties.privacyVersion(), acceptance.privacyVersion())) {
            throw new HappyGalleryException(
                    ErrorCode.POLICY_CONSENT_REQUIRED,
                    "현재 이용약관과 개인정보처리방침을 확인하고 동의해 주세요.");
        }
    }

    public record CurrentPolicy(String termsVersion, String privacyVersion) {}
}
