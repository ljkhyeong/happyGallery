package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** OAuth 인증이 끝난 신규 사용자의 가입 동의 대기 상태. 토큰은 보관하지 않는다. */
@Component
public class PendingSocialSignupStore {
    private static final String ATTRIBUTE = "pendingSocialSignup";
    private static final Duration TTL = Duration.ofMinutes(5);
    private final Clock clock;
    private final SessionStateCodec codec;

    public PendingSocialSignupStore(Clock clock, SessionStateCodec codec) {
        this.clock = clock;
        this.codec = codec;
    }

    public String save(HttpServletRequest request, SocialLoginCommand profile) {
        String attemptId = UUID.randomUUID().toString();
        request.getSession().setAttribute(ATTRIBUTE, codec.encode(new PendingSignup(
                attemptId, profile.withPolicyAcceptance(null), clock.instant().plus(TTL))));
        return attemptId;
    }

    public SocialLoginCommand consume(HttpServletRequest request, String attemptId) {
        HttpSession session = request.getSession(false);
        PendingSignup pending = session == null ? null
                : codec.decode(session.getAttribute(ATTRIBUTE), PendingSignup.class);
        if (pending == null || pending.expiresAt() == null
                || !clock.instant().isBefore(pending.expiresAt())) {
            if (session != null) clear(session);
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        if (!attemptId.equals(pending.attemptId()) || pending.profile() == null) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        clear(session);
        return pending.profile();
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(ATTRIBUTE);
    }

    private record PendingSignup(String attemptId, SocialLoginCommand profile, Instant expiresAt) {}
}
