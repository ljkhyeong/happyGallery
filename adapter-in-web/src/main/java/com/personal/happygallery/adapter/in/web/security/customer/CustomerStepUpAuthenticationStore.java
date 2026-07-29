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

    private static final String STEP_UP_AUTHENTICATION_ATTRIBUTE = "customerStepUpAuthentication";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final Clock clock;
    private final SessionStateCodec stateCodec;

    public CustomerStepUpAuthenticationStore(Clock clock, SessionStateCodec stateCodec) {
        this.clock = clock;
        this.stateCodec = stateCodec;
    }

    public void markVerified(HttpServletRequest request, Long userId, long credentialVersion) {
        request.getSession().setAttribute(
                STEP_UP_AUTHENTICATION_ATTRIBUTE,
                stateCodec.encode(new StepUpAuthentication(
                        userId,
                        credentialVersion,
                        Instant.now(clock).plus(TTL))));
    }

    public boolean isRecentlyVerified(
            HttpServletRequest request,
            Long userId,
            long credentialVersion) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        StepUpAuthentication authentication = stateCodec.decode(
                session.getAttribute(STEP_UP_AUTHENTICATION_ATTRIBUTE),
                StepUpAuthentication.class);
        boolean valid = authentication != null
                && userId.equals(authentication.userId())
                && credentialVersion == authentication.credentialVersion()
                && authentication.expiresAt() != null
                && Instant.now(clock).isBefore(authentication.expiresAt());
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
        session.removeAttribute(STEP_UP_AUTHENTICATION_ATTRIBUTE);
    }

    private record StepUpAuthentication(
            Long userId,
            long credentialVersion,
            Instant expiresAt
    ) {}
}
