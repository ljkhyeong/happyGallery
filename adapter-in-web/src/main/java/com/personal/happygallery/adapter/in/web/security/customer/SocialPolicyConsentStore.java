package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SocialPolicyConsentStore {

    public static final String TERMS_VERSION_PARAMETER = "termsVersion";
    public static final String TERMS_ACCEPTED_PARAMETER = "termsAccepted";
    public static final String PRIVACY_VERSION_PARAMETER = "privacyVersion";
    public static final String PRIVACY_ACCEPTED_PARAMETER = "privacyAccepted";

    private static final String TERMS_VERSION_ATTRIBUTE = "socialPolicyTermsVersion";
    private static final String TERMS_ACCEPTED_ATTRIBUTE = "socialPolicyTermsAccepted";
    private static final String PRIVACY_VERSION_ATTRIBUTE = "socialPolicyPrivacyVersion";
    private static final String PRIVACY_ACCEPTED_ATTRIBUTE = "socialPolicyPrivacyAccepted";
    private static final String OAUTH_STATE_ATTRIBUTE = "socialPolicyOauthState";
    private static final String EXPIRES_AT_ATTRIBUTE = "socialPolicyExpiresAt";
    private static final Duration CONSENT_TTL = Duration.ofMinutes(10);

    private final Clock clock;

    public SocialPolicyConsentStore(Clock clock) {
        this.clock = clock;
    }

    public void bindOauthState(HttpServletRequest request, String oauthState) {
        clear(request);
        if (!hasConsentParameters(request)) {
            return;
        }
        if (!StringUtils.hasText(oauthState)) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        HttpSession session = request.getSession();
        session.setAttribute(TERMS_VERSION_ATTRIBUTE, request.getParameter(TERMS_VERSION_PARAMETER));
        session.setAttribute(
                TERMS_ACCEPTED_ATTRIBUTE,
                Boolean.parseBoolean(request.getParameter(TERMS_ACCEPTED_PARAMETER)));
        session.setAttribute(PRIVACY_VERSION_ATTRIBUTE, request.getParameter(PRIVACY_VERSION_PARAMETER));
        session.setAttribute(
                PRIVACY_ACCEPTED_ATTRIBUTE,
                Boolean.parseBoolean(request.getParameter(PRIVACY_ACCEPTED_PARAMETER)));
        session.setAttribute(OAUTH_STATE_ATTRIBUTE, oauthState);
        session.setAttribute(
                EXPIRES_AT_ATTRIBUTE,
                Instant.now(clock).plus(CONSENT_TTL).toEpochMilli());
    }

    public PolicyAcceptance consume(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object termsVersion = session.getAttribute(TERMS_VERSION_ATTRIBUTE);
        Object termsAccepted = session.getAttribute(TERMS_ACCEPTED_ATTRIBUTE);
        Object privacyVersion = session.getAttribute(PRIVACY_VERSION_ATTRIBUTE);
        Object privacyAccepted = session.getAttribute(PRIVACY_ACCEPTED_ATTRIBUTE);
        Object oauthState = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTRIBUTE);
        boolean hasConsent = termsVersion != null
                || termsAccepted != null
                || privacyVersion != null
                || privacyAccepted != null
                || oauthState != null
                || expiresAt != null;
        clear(session);
        if (!hasConsent) {
            return null;
        }

        String callbackState = request.getParameter(OAuth2ParameterNames.STATE);
        if (!(termsVersion instanceof String terms)
                || !(termsAccepted instanceof Boolean acceptedTerms)
                || !(privacyVersion instanceof String privacy)
                || !(privacyAccepted instanceof Boolean acceptedPrivacy)
                || !(oauthState instanceof String storedState)
                || !(expiresAt instanceof Long expirationMillis)
                || !StringUtils.hasText(callbackState)
                || !storedState.equals(callbackState)
                || !Instant.now(clock).isBefore(Instant.ofEpochMilli(expirationMillis))) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return new PolicyAcceptance(terms, acceptedTerms, privacy, acceptedPrivacy);
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            clear(session);
        }
    }

    private boolean hasConsentParameters(HttpServletRequest request) {
        return request.getParameter(TERMS_VERSION_PARAMETER) != null
                || request.getParameter(TERMS_ACCEPTED_PARAMETER) != null
                || request.getParameter(PRIVACY_VERSION_PARAMETER) != null
                || request.getParameter(PRIVACY_ACCEPTED_PARAMETER) != null;
    }

    private static void clear(HttpSession session) {
        session.removeAttribute(TERMS_VERSION_ATTRIBUTE);
        session.removeAttribute(TERMS_ACCEPTED_ATTRIBUTE);
        session.removeAttribute(PRIVACY_VERSION_ATTRIBUTE);
        session.removeAttribute(PRIVACY_ACCEPTED_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(EXPIRES_AT_ATTRIBUTE);
    }
}
