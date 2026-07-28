package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SocialSignupIntentStore {

    public static final String SIGNUP_ATTEMPT_PARAMETER = "signupAttempt";

    private static final String TERMS_VERSION_ATTRIBUTE = "socialSignupTermsVersion";
    private static final String TERMS_ACCEPTED_ATTRIBUTE = "socialSignupTermsAccepted";
    private static final String PRIVACY_VERSION_ATTRIBUTE = "socialSignupPrivacyVersion";
    private static final String PRIVACY_ACCEPTED_ATTRIBUTE = "socialSignupPrivacyAccepted";
    private static final String PROVIDER_ATTRIBUTE = "socialSignupProvider";
    private static final String ATTEMPT_ID_ATTRIBUTE = "socialSignupAttemptId";
    private static final String OAUTH_STATE_ATTRIBUTE = "socialSignupOauthState";
    private static final String EXPIRES_AT_ATTRIBUTE = "socialSignupExpiresAt";
    private static final Duration SIGNUP_INTENT_TTL = Duration.ofMinutes(5);

    private final Clock clock;

    public SocialSignupIntentStore(Clock clock) {
        this.clock = clock;
    }

    public String start(HttpServletRequest request,
                        SocialProvider provider,
                        PolicyAcceptance acceptance) {
        HttpSession session = request.getSession();
        clear(session);
        String attemptId = UUID.randomUUID().toString();
        session.setAttribute(TERMS_VERSION_ATTRIBUTE, acceptance.termsVersion());
        session.setAttribute(TERMS_ACCEPTED_ATTRIBUTE, acceptance.termsAccepted());
        session.setAttribute(PRIVACY_VERSION_ATTRIBUTE, acceptance.privacyVersion());
        session.setAttribute(PRIVACY_ACCEPTED_ATTRIBUTE, acceptance.privacyAccepted());
        session.setAttribute(PROVIDER_ATTRIBUTE, provider.name());
        session.setAttribute(ATTEMPT_ID_ATTRIBUTE, attemptId);
        session.setAttribute(
                EXPIRES_AT_ATTRIBUTE,
                Instant.now(clock).plus(SIGNUP_INTENT_TTL).toEpochMilli());
        return attemptId;
    }

    public boolean bindOauthState(HttpServletRequest request,
                                  String attemptId,
                                  SocialProvider provider,
                                  String oauthState) {
        HttpSession session = request.getSession(false);
        if (session == null || !StringUtils.hasText(oauthState)) {
            return false;
        }

        Object termsVersion = session.getAttribute(TERMS_VERSION_ATTRIBUTE);
        Object termsAccepted = session.getAttribute(TERMS_ACCEPTED_ATTRIBUTE);
        Object privacyVersion = session.getAttribute(PRIVACY_VERSION_ATTRIBUTE);
        Object privacyAccepted = session.getAttribute(PRIVACY_ACCEPTED_ATTRIBUTE);
        Object storedProvider = session.getAttribute(PROVIDER_ATTRIBUTE);
        Object storedAttemptId = session.getAttribute(ATTEMPT_ID_ATTRIBUTE);
        Object boundState = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTRIBUTE);

        boolean valid = termsVersion instanceof String
                && termsAccepted instanceof Boolean
                && privacyVersion instanceof String
                && privacyAccepted instanceof Boolean
                && storedProvider instanceof String providerName
                && storedAttemptId instanceof String storedId
                && expiresAt instanceof Long expirationMillis
                && boundState == null
                && storedId.equals(attemptId)
                && provider.name().equals(providerName)
                && Instant.now(clock).isBefore(Instant.ofEpochMilli(expirationMillis));
        if (!valid) {
            clear(session);
            return false;
        }

        session.setAttribute(OAUTH_STATE_ATTRIBUTE, oauthState);
        return true;
    }

    public Optional<PolicyAcceptance> consume(HttpServletRequest request,
                                              SocialProvider callbackProvider) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object termsVersion = session.getAttribute(TERMS_VERSION_ATTRIBUTE);
        Object termsAccepted = session.getAttribute(TERMS_ACCEPTED_ATTRIBUTE);
        Object privacyVersion = session.getAttribute(PRIVACY_VERSION_ATTRIBUTE);
        Object privacyAccepted = session.getAttribute(PRIVACY_ACCEPTED_ATTRIBUTE);
        Object provider = session.getAttribute(PROVIDER_ATTRIBUTE);
        Object attemptId = session.getAttribute(ATTEMPT_ID_ATTRIBUTE);
        Object oauthState = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTRIBUTE);
        boolean hasIntent = termsVersion != null
                || termsAccepted != null
                || privacyVersion != null
                || privacyAccepted != null
                || provider != null
                || attemptId != null
                || oauthState != null
                || expiresAt != null;
        clear(session);
        if (!hasIntent) {
            return Optional.empty();
        }

        String callbackState = request.getParameter(OAuth2ParameterNames.STATE);
        if (!(termsVersion instanceof String terms)
                || !(termsAccepted instanceof Boolean acceptedTerms)
                || !(privacyVersion instanceof String privacy)
                || !(privacyAccepted instanceof Boolean acceptedPrivacy)
                || !(provider instanceof String storedProvider)
                || !(attemptId instanceof String)
                || !(oauthState instanceof String storedState)
                || !(expiresAt instanceof Long expirationMillis)
                || !StringUtils.hasText(callbackState)
                || !storedState.equals(callbackState)
                || !callbackProvider.name().equals(storedProvider)
                || !Instant.now(clock).isBefore(Instant.ofEpochMilli(expirationMillis))) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return Optional.of(new PolicyAcceptance(terms, acceptedTerms, privacy, acceptedPrivacy));
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            clear(session);
        }
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(TERMS_VERSION_ATTRIBUTE);
        session.removeAttribute(TERMS_ACCEPTED_ATTRIBUTE);
        session.removeAttribute(PRIVACY_VERSION_ATTRIBUTE);
        session.removeAttribute(PRIVACY_ACCEPTED_ATTRIBUTE);
        session.removeAttribute(PROVIDER_ATTRIBUTE);
        session.removeAttribute(ATTEMPT_ID_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(EXPIRES_AT_ATTRIBUTE);
    }
}
