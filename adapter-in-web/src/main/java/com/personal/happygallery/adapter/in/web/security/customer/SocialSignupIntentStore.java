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

    private static final String SIGNUP_INTENT_ATTRIBUTE = "socialSignupIntent";
    private static final Duration SIGNUP_INTENT_TTL = Duration.ofMinutes(5);

    private final Clock clock;
    private final SessionStateCodec stateCodec;

    public SocialSignupIntentStore(Clock clock, SessionStateCodec stateCodec) {
        this.clock = clock;
        this.stateCodec = stateCodec;
    }

    public String start(HttpServletRequest request,
                        SocialProvider provider,
                        PolicyAcceptance acceptance) {
        HttpSession session = request.getSession();
        clear(session);
        String attemptId = UUID.randomUUID().toString();
        session.setAttribute(
                SIGNUP_INTENT_ATTRIBUTE,
                stateCodec.encode(new SignupIntentState(
                        acceptance.termsVersion(),
                        acceptance.termsAccepted(),
                        acceptance.privacyVersion(),
                        acceptance.privacyAccepted(),
                        provider,
                        attemptId,
                        null,
                        Instant.now(clock).plus(SIGNUP_INTENT_TTL))));
        return attemptId;
    }

    public boolean bindOauthState(HttpServletRequest request,
                                  String attemptId,
                                  SocialProvider provider,
                                  String oauthState) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        if (!StringUtils.hasText(oauthState)) {
            clear(session);
            return false;
        }

        SignupIntentState intent = stateCodec.decode(
                session.getAttribute(SIGNUP_INTENT_ATTRIBUTE),
                SignupIntentState.class);
        if (intent == null
                || intent.termsVersion() == null
                || intent.privacyVersion() == null
                || intent.provider() == null
                || intent.attemptId() == null
                || intent.oauthState() != null
                || intent.expiresAt() == null
                || !intent.attemptId().equals(attemptId)
                || provider != intent.provider()
                || !Instant.now(clock).isBefore(intent.expiresAt())) {
            clear(session);
            return false;
        }

        session.setAttribute(
                SIGNUP_INTENT_ATTRIBUTE,
                stateCodec.encode(intent.bindOauthState(oauthState)));
        return true;
    }

    public Optional<PolicyAcceptance> consume(HttpServletRequest request,
                                              SocialProvider callbackProvider) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object storedIntent = session.getAttribute(SIGNUP_INTENT_ATTRIBUTE);
        clear(session);
        if (storedIntent == null) {
            return Optional.empty();
        }

        String callbackState = request.getParameter(OAuth2ParameterNames.STATE);
        SignupIntentState intent = stateCodec.decode(storedIntent, SignupIntentState.class);
        if (intent == null
                || intent.termsVersion() == null
                || intent.privacyVersion() == null
                || intent.provider() == null
                || intent.attemptId() == null
                || intent.oauthState() == null
                || intent.expiresAt() == null
                || !StringUtils.hasText(callbackState)
                || !intent.oauthState().equals(callbackState)
                || callbackProvider != intent.provider()
                || !Instant.now(clock).isBefore(intent.expiresAt())) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return Optional.of(new PolicyAcceptance(
                intent.termsVersion(),
                intent.termsAccepted(),
                intent.privacyVersion(),
                intent.privacyAccepted()));
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            clear(session);
        }
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(SIGNUP_INTENT_ATTRIBUTE);
    }

    private record SignupIntentState(
            String termsVersion,
            boolean termsAccepted,
            String privacyVersion,
            boolean privacyAccepted,
            SocialProvider provider,
            String attemptId,
            String oauthState,
            Instant expiresAt
    ) {

        private SignupIntentState bindOauthState(String oauthState) {
            return new SignupIntentState(
                    termsVersion,
                    termsAccepted,
                    privacyVersion,
                    privacyAccepted,
                    provider,
                    attemptId,
                    oauthState,
                    expiresAt);
        }
    }
}
