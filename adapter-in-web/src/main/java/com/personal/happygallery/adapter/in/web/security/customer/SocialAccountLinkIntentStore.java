package com.personal.happygallery.adapter.in.web.security.customer;

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
public class SocialAccountLinkIntentStore {

    public static final String LINK_ATTEMPT_PARAMETER = "linkAttempt";

    private static final String USER_ID_ATTRIBUTE = "socialAccountLinkUserId";
    private static final String CREDENTIAL_VERSION_ATTRIBUTE = "socialAccountLinkCredentialVersion";
    private static final String PROVIDER_ATTRIBUTE = "socialAccountLinkProvider";
    private static final String EXPIRES_AT_ATTRIBUTE = "socialAccountLinkExpiresAt";
    private static final String ATTEMPT_ID_ATTRIBUTE = "socialAccountLinkAttemptId";
    private static final String OAUTH_STATE_ATTRIBUTE = "socialAccountLinkOauthState";
    private static final String PURPOSE_ATTRIBUTE = "socialAccountLinkPurpose";
    private static final Duration LINK_INTENT_TTL = Duration.ofMinutes(5);

    private final Clock clock;

    public SocialAccountLinkIntentStore(Clock clock) {
        this.clock = clock;
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
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
        session.setAttribute(CREDENTIAL_VERSION_ATTRIBUTE, credentialVersion);
        session.setAttribute(PROVIDER_ATTRIBUTE, provider.name());
        session.setAttribute(EXPIRES_AT_ATTRIBUTE, Instant.now(clock).plus(LINK_INTENT_TTL).toEpochMilli());
        session.setAttribute(ATTEMPT_ID_ATTRIBUTE, attemptId);
        session.setAttribute(PURPOSE_ATTRIBUTE, purpose.name());
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

        Object linkedUserId = session.getAttribute(USER_ID_ATTRIBUTE);
        Object linkedCredentialVersion = session.getAttribute(CREDENTIAL_VERSION_ATTRIBUTE);
        Object linkedProvider = session.getAttribute(PROVIDER_ATTRIBUTE);
        Object expiration = session.getAttribute(EXPIRES_AT_ATTRIBUTE);
        Object linkedAttemptId = session.getAttribute(ATTEMPT_ID_ATTRIBUTE);
        Object boundState = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        Object purpose = session.getAttribute(PURPOSE_ATTRIBUTE);
        Object currentUserId = session.getAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        Object currentCredentialVersion = session.getAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE);

        boolean valid = linkedUserId instanceof Long userId
                && linkedCredentialVersion instanceof Long credentialVersion
                && linkedProvider instanceof String providerName
                && expiration instanceof Long expirationMillis
                && linkedAttemptId instanceof String storedAttemptId
                && boundState == null
                && purpose instanceof String
                && storedAttemptId.equals(attemptId)
                && provider.name().equals(providerName)
                && userId.equals(currentUserId)
                && credentialVersion.equals(currentCredentialVersion)
                && Instant.now(clock).isBefore(Instant.ofEpochMilli(expirationMillis));
        if (!valid) {
            clear(session);
            return false;
        }

        session.setAttribute(OAUTH_STATE_ATTRIBUTE, oauthState);
        return true;
    }

    public Optional<LinkIntent> consume(HttpServletRequest request, SocialProvider callbackProvider) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object userId = session.getAttribute(USER_ID_ATTRIBUTE);
        Object credentialVersion = session.getAttribute(CREDENTIAL_VERSION_ATTRIBUTE);
        Object provider = session.getAttribute(PROVIDER_ATTRIBUTE);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTRIBUTE);
        Object attemptId = session.getAttribute(ATTEMPT_ID_ATTRIBUTE);
        Object oauthState = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        Object purpose = session.getAttribute(PURPOSE_ATTRIBUTE);
        String callbackState = request.getParameter(OAuth2ParameterNames.STATE);
        Object currentUserId = session.getAttribute(CustomerAuthenticationFilter.CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        Object currentCredentialVersion = session.getAttribute(
                CustomerAuthenticationFilter.CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE);
        boolean hasIntent = userId != null
                || credentialVersion != null
                || provider != null
                || expiresAt != null
                || attemptId != null
                || oauthState != null
                || purpose != null;
        clear(session);
        if (!hasIntent) {
            return Optional.empty();
        }
        if (!(userId instanceof Long linkedUserId)
                || !(credentialVersion instanceof Long linkedCredentialVersion)
                || !(provider instanceof String linkedProvider)
                || !(expiresAt instanceof Long expirationMillis)
                || !(attemptId instanceof String)
                || !(oauthState instanceof String linkedOauthState)
                || !(purpose instanceof String linkedPurpose)
                || !StringUtils.hasText(callbackState)
                || !linkedOauthState.equals(callbackState)
                || !callbackProvider.name().equals(linkedProvider)
                || !linkedUserId.equals(currentUserId)
                || !linkedCredentialVersion.equals(currentCredentialVersion)
                || !Instant.now(clock).isBefore(Instant.ofEpochMilli(expirationMillis))) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        try {
            return Optional.of(new LinkIntent(
                    linkedUserId,
                    linkedCredentialVersion,
                    IntentPurpose.valueOf(linkedPurpose)));
        } catch (IllegalArgumentException exception) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
    }

    public void clear(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            clear(session);
        }
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(USER_ID_ATTRIBUTE);
        session.removeAttribute(CREDENTIAL_VERSION_ATTRIBUTE);
        session.removeAttribute(PROVIDER_ATTRIBUTE);
        session.removeAttribute(EXPIRES_AT_ATTRIBUTE);
        session.removeAttribute(ATTEMPT_ID_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(PURPOSE_ATTRIBUTE);
    }

    public enum IntentPurpose {
        LINK,
        REAUTHENTICATE
    }

    public record LinkIntent(
            Long userId,
            long credentialVersion,
            IntentPurpose purpose) {}
}
