package com.personal.happygallery.app.token;

import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.app.token.AccessTokenHasher;
import com.personal.happygallery.app.token.AccessTokenSigner;
import com.personal.happygallery.app.token.InvalidTokenException;
import com.personal.happygallery.config.properties.GuestTokenProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 게스트 접근 토큰 발급·검증 서비스.
 *
 * <p>신규 토큰은 HMAC-SHA256 서명 + 만료 기반으로 발급한다.
 * 기존 레거시 토큰(서명 없는 32자 hex)은 SHA-256 해시 비교 경로로 폴백한다.
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

    /** 서명된 토큰을 발급하고, DB 저장용 nonce 해시를 반환한다. */
    public IssuedToken issue() {
        Instant expiry = clock.instant().plus(Duration.ofHours(properties.expiryHours()));
        AccessTokenSigner.SignedToken signed = AccessTokenSigner.sign(expiry, properties.hmacSecret());
        return new IssuedToken(signed.rawToken(), signed.nonceHash());
    }

    /**
     * 토큰에서 DB 검색용 해시를 추출한다.
     * 서명된 토큰이면 HMAC 검증 + 만료 확인 후 nonce 해시를 반환한다.
     * 레거시 토큰이면 전체 토큰의 SHA-256 해시를 반환한다.
     *
     * @throws NotFoundException 서명 불일치, 만료, 형식 오류 시 (정보 노출 방지)
     */
    public String resolveTokenHash(String rawToken) {
        if (AccessTokenSigner.isSigned(rawToken)) {
            try {
                AccessTokenSigner.TokenClaims claims = AccessTokenSigner.verify(
                        rawToken, properties.hmacSecret(), clock.instant());
                return claims.nonceHash();
            } catch (InvalidTokenException e) {
                log.warn("토큰 검증 실패: {}", e.getMessage());
                throw new NotFoundException("접근 토큰");
            }
        }
        return AccessTokenHasher.hash(rawToken);
    }

    public record IssuedToken(String rawToken, String tokenHash) {}
}
