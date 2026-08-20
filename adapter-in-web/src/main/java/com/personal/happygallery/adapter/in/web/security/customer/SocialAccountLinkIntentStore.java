package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SocialAccountLinkIntentStore {

    public static final String LINK_ATTEMPT_PARAMETER = "linkAttempt";

    private static final String LINK_INTENT_ATTRIBUTE = "socialAccountLinkIntent";
    private static final Duration LINK_INTENT_TTL = Duration.ofMinutes(5);

    private final Clock clock;
    private final SessionStateCodec stateCodec;

    public SocialAccountLinkIntentStore(Clock clock, SessionStateCodec stateCodec) {
        this.clock = clock;
        this.stateCodec = stateCodec;
    }

    public String start(HttpServletRequest request,
                        Long userId,
                        long credentialVersion,
                        SocialProvider provider) {
        return start(request, userId, credentialVersion, provider, IntentPurpose.LINK);
    }

    public String startReauthentication(HttpServletRequest request,
                                        Long userId,
                                        long credentialVersion,
                                        SocialProvider provider) {
        return start(request, userId, credentialVersion, provider, IntentPurpose.REAUTHENTICATE);
    }

    private String start(HttpServletRequest request,
                         Long userId,
                         long credentialVersion,
                         SocialProvider provider,
                         IntentPurpose purpose) {
        HttpSession session = request.getSession();
        clear(session);
        String attemptId = UUID.randomUUID().toString();
        session.setAttribute(
                LINK_INTENT_ATTRIBUTE,
                stateCodec.encode(new LinkIntentState(
                        userId,
                        credentialVersion,
                        provider,
                        Instant.now(clock).plus(LINK_INTENT_TTL),
                        attemptId,
                        null,
                        purpose)));
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

        Object currentUserId = session.getAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        Object currentCredentialVersion = session.getAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE);

        LinkIntentState intent = stateCodec.decode(
                session.getAttribute(LINK_INTENT_ATTRIBUTE),
                LinkIntentState.class);
        if (intent == null
                || intent.userId() == null
                || intent.provider() == null
                || intent.expiresAt() == null
                || intent.attemptId() == null
                || intent.oauthState() != null
                || intent.purpose() == null
                || !intent.attemptId().equals(attemptId)
                || provider != intent.provider()
                || !intent.userId().equals(currentUserId)
                || !Objects.equals(intent.credentialVersion(), currentCredentialVersion)
                || !Instant.now(clock).isBefore(intent.expiresAt())) {
            clear(session);
            return false;
        }

        session.setAttribute(
                LINK_INTENT_ATTRIBUTE,
                stateCodec.encode(intent.bindOauthState(oauthState)));
        return true;
    }

    public Optional<LinkIntent> consume(HttpServletRequest request, SocialProvider callbackProvider) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object storedIntent = session.getAttribute(LINK_INTENT_ATTRIBUTE);
        String callbackState = request.getParameter(OAuth2ParameterNames.STATE);
        Object currentUserId = session.getAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        Object currentCredentialVersion = session.getAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE);
        clear(session);
        if (storedIntent == null) {
            return Optional.empty();
        }
        LinkIntentState intent = stateCodec.decode(storedIntent, LinkIntentState.class);
        if (intent == null
                || intent.userId() == null
                || intent.provider() == null
                || intent.expiresAt() == null
                || intent.attemptId() == null
                || intent.oauthState() == null
                || intent.purpose() == null
                || !StringUtils.hasText(callbackState)
                || !intent.oauthState().equals(callbackState)
                || callbackProvider != intent.provider()
                || !intent.userId().equals(currentUserId)
                || !Objects.equals(intent.credentialVersion(), currentCredentialVersion)
                || !Instant.now(clock).isBefore(intent.expiresAt())) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return Optional.of(new LinkIntent(
                intent.userId(),
                intent.credentialVersion(),
                intent.purpose()));
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            clear(session);
        }
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(LINK_INTENT_ATTRIBUTE);
    }

    public enum IntentPurpose {
        LINK,
        REAUTHENTICATE
    }

    public record LinkIntent(
            Long userId,
            long credentialVersion,
            IntentPurpose purpose) {}

    private record LinkIntentState(
            Long userId,
            long credentialVersion,
            SocialProvider provider,
            Instant expiresAt,
            String attemptId,
            String oauthState,
            IntentPurpose purpose
    ) {

        private LinkIntentState bindOauthState(String oauthState) {
            return new LinkIntentState(
                    userId,
                    credentialVersion,
                    provider,
                    expiresAt,
                    attemptId,
                    oauthState,
                    purpose);
        }
    }
}
