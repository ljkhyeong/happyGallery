package com.personal.happygallery.application.token;

import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 게스트 접근 토큰 발급·검증 서비스.
 *
 * <p>토큰은 HMAC-SHA256 서명 + 만료 기반으로 발급한다.
 * 토큰 파싱 오류는 {@link InvalidTokenException}으로 구분하고, 외부에는 NotFound로 변환한다.
 */
@Component
public class GuestTokenService {

    private static final Logger log = LoggerFactory.getLogger(GuestTokenService.class);

    private final GuestTokenProperties properties;
    private final Clock clock;

    public GuestTokenService(GuestTokenProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /** 서명된 토큰을 발급하고, DB 저장용 전체 토큰 해시를 반환한다. */
    public IssuedToken issue() {
        return issue(properties.accessExpiry());
    }

    /** 휴대폰 재인증 뒤 사용할 수명이 짧은 관리 토큰을 발급한다. */
    public IssuedToken issueRecoveryToken() {
        return issue(properties.recoveryExpiry());
    }

    /** 결제 결과 보존 기간 동안 비회원이 결제·보상환불 상태를 조회할 토큰을 발급한다. */
    public IssuedToken issuePaymentStatusToken() {
        return issue(properties.accessExpiry());
    }

    private IssuedToken issue(Duration validity) {
        Instant expiry = clock.instant().plus(validity);
        AccessTokenSigner.SignedToken signed = AccessTokenSigner.sign(expiry, properties.hmacSecret());
        return new IssuedToken(signed.rawToken(), signed.tokenHash(), expiry);
    }

    /**
     * 토큰에서 DB 검색용 해시를 추출한다.
     * HMAC 검증 + 만료 확인 후 전체 토큰 해시를 반환한다.
     *
     * @throws NotFoundException 서명 불일치, 만료, 형식 오류 시 (정보 노출 방지)
     */
    public String resolveTokenHash(String rawToken) {
        try {
            verifySigned(rawToken);
            return AccessTokenHasher.hash(rawToken);
        } catch (InvalidTokenException e) {
            log.warn("게스트 토큰 검증 실패 [type={}]", e.getClass().getSimpleName());
            throw new NotFoundException("접근 토큰");
        }
    }

    private AccessTokenSigner.TokenClaims verifySigned(String rawToken) {
        Instant now = clock.instant();
        try {
            return AccessTokenSigner.verify(rawToken, properties.hmacSecret(), now);
        } catch (InvalidTokenException activeKeyFailure) {
            if (properties.previousHmacSecret().isBlank()) {
                throw activeKeyFailure;
            }
            return AccessTokenSigner.verify(rawToken, properties.previousHmacSecret(), now);
        }
    }

    public record IssuedToken(String rawToken, String tokenHash, Instant expiresAt) {}
}
