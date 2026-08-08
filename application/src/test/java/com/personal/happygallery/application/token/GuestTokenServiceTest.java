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
            softly.assertThat(issued.tokenHash()).isEqualTo(AccessTokenHasher.hash(issued.rawToken()));
            softly.assertThat(claims.expiry()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        });
        assertThatThrownBy(() -> AccessTokenSigner.verify(issued.rawToken(), PREVIOUS_SECRET, NOW))
                .isInstanceOf(InvalidTokenException.class);
    }

    @DisplayName("복구용 관리 토큰은 일반 비회원 토큰보다 짧게 만료한다")
    @Test
    void issueRecoveryToken_usesRecoveryExpiry() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);

        GuestTokenService.IssuedToken issued = service.issueRecoveryToken();

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(AccessTokenSigner.verify(issued.rawToken(), ACTIVE_SECRET, NOW).expiry())
                .isEqualTo(issued.expiresAt());
    }

    @DisplayName("결제 상태 조회 토큰은 결과 보존 기간 뒤 만료한다")
    @Test
    void issuePaymentStatusToken_expiresAfterThirtyDays() {
        GuestTokenService.IssuedToken issued = service(ACTIVE_SECRET, PREVIOUS_SECRET)
                .issuePaymentStatusToken();

        assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(720)));
        GuestTokenService expiredTokenVerifier = serviceAt(
                ACTIVE_SECRET, PREVIOUS_SECRET, NOW.plus(Duration.ofHours(720)));
        assertThatThrownBy(() -> expiredTokenVerifier.resolveTokenHash(issued.rawToken()))
                .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("이전 키로 발급된 기존 서명 토큰도 검증한다")
    @Test
    void resolveTokenHash_tokenSignedWithPreviousSecret_returnsTokenHash() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);
        AccessTokenSigner.SignedToken previousToken = AccessTokenSigner.sign(
                NOW.plus(Duration.ofHours(1)), PREVIOUS_SECRET);

        assertThat(service.resolveTokenHash(previousToken.rawToken()))
                .isEqualTo(previousToken.tokenHash());
    }

    @DisplayName("서명 토큰에서 추출한 nonce만 제출하면 거절한다")
    @Test
    void resolveTokenHash_extractedNonce_throwsNotFoundException() {
        GuestTokenService service = service(ACTIVE_SECRET, PREVIOUS_SECRET);
        GuestTokenService.IssuedToken issued = service.issue();
        String nonce = AccessTokenSigner.verify(issued.rawToken(), ACTIVE_SECRET, NOW).nonce();

        assertThatThrownBy(() -> service.resolveTokenHash(nonce))
                .isInstanceOf(NotFoundException.class);
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

    @DisplayName("서명 없는 레거시 토큰은 거절한다")
    @Test
    void resolveTokenHash_legacyToken_throwsNotFoundException() {
        GuestTokenService service = service(ACTIVE_SECRET, "");
        String legacyToken = "0123456789abcdef0123456789abcdef";

        assertThatThrownBy(() -> service.resolveTokenHash(legacyToken))
                .isInstanceOf(NotFoundException.class);
    }

    private GuestTokenService service(String activeSecret, String previousSecret) {
        return serviceAt(activeSecret, previousSecret, NOW);
    }

    private GuestTokenService serviceAt(String activeSecret, String previousSecret, Instant now) {
        GuestTokenProperties properties = new GuestTokenProperties(
                activeSecret, previousSecret, Duration.ofHours(720), Duration.ofHours(24));
        return new GuestTokenService(properties, Clock.fixed(now, UTC));
    }
}
