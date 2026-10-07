package com.personal.happygallery.application.token;

import com.personal.happygallery.domain.crypto.Sha256;
import com.personal.happygallery.domain.error.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
            return Sha256.hex(rawToken);
        } catch (InvalidTokenException e) {
            log.warn("게스트 토큰 검증 실패 [type={}]", e.getClass().getSimpleName());
            throw new NotFoundException("접근 토큰");
        }
    }

    /**
     * 원문 토큰의 서명·만료를 검증하고, 전체 토큰 해시가 저장 해시와 같은지 상수 시간으로 비교한다.
     * 형식 오류·서명 불일치·만료는 false로 처리한다.
     */
    public boolean matches(String rawToken, String storedHash) {
        if (!StringUtils.hasText(rawToken) || !StringUtils.hasText(storedHash)) {
            return false;
        }
        try {
            verifySigned(rawToken);
        } catch (InvalidTokenException e) {
            log.warn("게스트 토큰 검증 실패 [type={}]", e.getClass().getSimpleName());
            return false;
        }
        return constantTimeEquals(Sha256.hex(rawToken), storedHash);
    }

    /** 활성 HMAC 키로 서버 발급 증거의 서명을 만든다. */
    public String signPayload(String data) {
        return AccessTokenSigner.hmac(data, properties.hmacSecret());
    }

    /** 활성 키, 그다음 설정된 이전 키 순서로 서명을 검증한다. */
    public boolean matchesPayloadSignature(String data, String signature) {
        if (signature == null) {
            return false;
        }
        if (constantTimeEquals(signature, AccessTokenSigner.hmac(data, properties.hmacSecret()))) {
            return true;
        }
        return !properties.previousHmacSecret().isBlank()
                && constantTimeEquals(signature, AccessTokenSigner.hmac(data, properties.previousHmacSecret()));
    }

    private static boolean constantTimeEquals(String actual, String expected) {
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
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
