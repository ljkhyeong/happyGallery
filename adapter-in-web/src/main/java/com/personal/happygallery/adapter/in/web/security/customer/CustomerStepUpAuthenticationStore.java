package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** 민감한 계정 변경 전에 완료한 최근 본인 확인을 현재 세션에만 보관한다. */
@Component
public class CustomerStepUpAuthenticationStore {

    private static final String USER_ID_ATTRIBUTE = "customerStepUpUserId";
    private static final String CREDENTIAL_VERSION_ATTRIBUTE = "customerStepUpCredentialVersion";
    private static final String EXPIRES_AT_ATTRIBUTE = "customerStepUpExpiresAt";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final Clock clock;

    public CustomerStepUpAuthenticationStore(Clock clock) {
        this.clock = clock;
    }

    public void markVerified(HttpServletRequest request, Long userId, long credentialVersion) {
        HttpSession session = request.getSession();
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
        session.setAttribute(CREDENTIAL_VERSION_ATTRIBUTE, credentialVersion);
        session.setAttribute(EXPIRES_AT_ATTRIBUTE, Instant.now(clock).plus(TTL).toEpochMilli());
    }

    public boolean isRecentlyVerified(
            HttpServletRequest request,
            Long userId,
            long credentialVersion) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        boolean valid = userId.equals(session.getAttribute(USER_ID_ATTRIBUTE))
                && Long.valueOf(credentialVersion).equals(
                        session.getAttribute(CREDENTIAL_VERSION_ATTRIBUTE))
                && session.getAttribute(EXPIRES_AT_ATTRIBUTE) instanceof Long expiresAt
                && Instant.now(clock).isBefore(Instant.ofEpochMilli(expiresAt));
        if (!valid) {
            clear(session);
        }
        return valid;
    }

    public void requireRecentlyVerified(
            HttpServletRequest request,
            Long userId,
            long credentialVersion) {
        if (!isRecentlyVerified(request, userId, credentialVersion)) {
            throw new HappyGalleryException(ErrorCode.REAUTHENTICATION_REQUIRED);
        }
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(USER_ID_ATTRIBUTE);
        session.removeAttribute(CREDENTIAL_VERSION_ATTRIBUTE);
        session.removeAttribute(EXPIRES_AT_ATTRIBUTE);
    }
}
