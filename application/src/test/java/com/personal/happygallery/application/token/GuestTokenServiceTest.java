package com.personal.happygallery.application.token;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GuestTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-01T00:00:00Z");
    private static final String ACTIVE_SECRET = "active-guest-token-secret-at-least-32-bytes";
    private static final String PREVIOUS_SECRET = "previous-guest-token-secret-at-least-32-bytes";
    private static final String UNKNOWN_SECRET = "unknown-guest-token-secret-at-least-32-bytes";

    @DisplayName("신규 토큰은 활성 키로만 서명하고 설정된 시간 뒤 만료한다")
    @Test
    void issue_signsWithActiveSecret() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);

        GuestTokenService.IssuedToken issued = service.issue();
        AccessTokenSigner.TokenClaims claims = AccessTokenSigner.verify(
                issued.rawToken(), ACTIVE_SECRET, NOW);

        assertSoftly(softly -> {
            softly.assertThat(claims.nonceHash()).isEqualTo(issued.tokenHash());
            softly.assertThat(claims.expiry()).isEqualTo(NOW.plus(Duration.ofHours(168)));
        });
        assertThatThrownBy(() -> AccessTokenSigner.verify(issued.rawToken(), PREVIOUS_SECRET, NOW))
                .isInstanceOf(InvalidTokenException.class);
    }

    @DisplayName("이전 키로 발급된 기존 서명 토큰도 검증한다")
    @Test
    void resolveTokenHash_tokenSignedWithPreviousSecret_returnsNonceHash() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);
        AccessTokenSigner.SignedToken previousToken = AccessTokenSigner.sign(
                NOW.plus(Duration.ofHours(1)), PREVIOUS_SECRET);

        assertThat(service.resolveTokenHash(previousToken.rawToken()))
                .isEqualTo(previousToken.nonceHash());
    }

    @DisplayName("활성 키와 이전 키에 모두 맞지 않는 서명 토큰은 찾을 수 없음으로 변환한다")
    @Test
    void resolveTokenHash_tokenSignedWithUnknownSecret_throwsNotFoundException() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);
        AccessTokenSigner.SignedToken unknownToken = AccessTokenSigner.sign(
                NOW.plus(Duration.ofHours(1)), UNKNOWN_SECRET);

        assertThatThrownBy(() -> service.resolveTokenHash(unknownToken.rawToken()))
                .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("서명 없는 레거시 토큰은 기존처럼 전체 토큰 해시를 반환한다")
    @Test
    void resolveTokenHash_legacyToken_returnsRawTokenHash() {
        GuestTokenService service = service(ACTIVE_SECRET, "");
        String legacyToken = "0123456789abcdef0123456789abcdef";

        assertThat(service.resolveTokenHash(legacyToken))
                .isEqualTo(AccessTokenHasher.hash(legacyToken));
    }

    private GuestTokenService service(String activeSecret, String previousSecret) {
        GuestTokenProperties properties = new GuestTokenProperties(activeSecret, previousSecret, 168);
        return new GuestTokenService(properties, Clock.fixed(NOW, UTC));
    }
}
