package com.personal.happygallery.application.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.personal.happygallery.domain.error.ErrorCode;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AccessTokenSignerTest {

    private static final String SECRET = "test-hmac-secret-key-at-least-32-bytes-long-for-security";

    @DisplayName("서명 토큰은 전체 토큰 해시로 저장하고 검증 후 만료 시각을 반환한다")
    @Test
    void signAndVerify_returnsClaims() {
        Instant expiry = Instant.parse("2026-05-01T00:00:00Z");
        AccessTokenSigner.SignedToken signed = AccessTokenSigner.sign(expiry, SECRET);

        AccessTokenSigner.TokenClaims claims = AccessTokenSigner.verify(
                signed.rawToken(), SECRET, Instant.parse("2026-04-30T00:00:00Z"));

        assertSoftly(softly -> {
            softly.assertThat(claims.expiry()).isEqualTo(expiry);
            softly.assertThat(signed.tokenHash()).isEqualTo(AccessTokenHasher.hash(signed.rawToken()));
        });
    }

    @DisplayName("서명이 변조된 토큰은 InvalidTokenException으로 거절한다")
    @Test
    void verify_tamperedSignature_throwsInvalidTokenException() {
        AccessTokenSigner.SignedToken signed = AccessTokenSigner.sign(
                Instant.parse("2026-05-01T00:00:00Z"), SECRET);
        String tampered = signed.rawToken() + "x";

        assertThatThrownBy(() -> AccessTokenSigner.verify(
                tampered, SECRET, Instant.parse("2026-04-30T00:00:00Z")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("토큰 서명 불일치");
    }

    @DisplayName("서명 토큰은 만료 시각부터 거절한다")
    @Test
    void verify_atExpiry_throwsInvalidTokenException() {
        Instant expiry = Instant.parse("2026-05-01T00:00:00Z");
        AccessTokenSigner.SignedToken signed = AccessTokenSigner.sign(expiry, SECRET);

        assertThat(AccessTokenSigner.verify(signed.rawToken(), SECRET, expiry.minusNanos(1)).expiry())
                .isEqualTo(expiry);
        assertThatThrownBy(() -> AccessTokenSigner.verify(signed.rawToken(), SECRET, expiry))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("토큰 만료");
    }

    @DisplayName("서명이 유효해도 Base64와 만료 시각 형식이 깨진 페이로드는 형식 오류로 거절한다")
    @Test
    void verify_malformedPayload_throwsInvalidTokenException() throws Exception {
        String invalidBase64Token = signedToken("%");
        String invalidEpochPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("nonce:not-a-number".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> AccessTokenSigner.verify(
                invalidBase64Token, SECRET, Instant.parse("2026-04-30T00:00:00Z")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("잘못된 토큰 페이로드");
        assertThatThrownBy(() -> AccessTokenSigner.verify(
                signedToken(invalidEpochPayload), SECRET, Instant.parse("2026-04-30T00:00:00Z")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("잘못된 토큰 페이로드");
    }

    @DisplayName("서명 비밀키가 잘못되면 스택트레이스 없는 TokenSigningException을 던진다")
    @Test
    void sign_invalidSecret_throwsTokenSigningException() {
        assertThatThrownBy(() -> AccessTokenSigner.sign(Instant.parse("2026-05-01T00:00:00Z"), ""))
                .isInstanceOfSatisfying(TokenSigningException.class, exception -> {
                    assertSoftly(softly -> {
                        softly.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
                        softly.assertThat(exception.getStackTrace()).isEmpty();
                    });
                });
    }

    private static String signedToken(String encodedPayload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
        return encodedPayload + "." + signature;
    }
}
