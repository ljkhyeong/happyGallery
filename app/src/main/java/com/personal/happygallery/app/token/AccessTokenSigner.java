package com.personal.happygallery.app.token;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 서명 + 만료 기반 guest access token 유틸.
 *
 * <p>토큰 형식: {@code base64url(nonce:expiryEpochSeconds).base64url(hmac)}
 * <ul>
 *   <li>nonce: 16바이트 랜덤 hex (32자)</li>
 *   <li>DB에는 nonce의 SHA-256 해시를 저장하여 기존 컬럼과 호환</li>
 * </ul>
 */
public final class AccessTokenSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private AccessTokenSigner() {}

    /**
     * 서명된 토큰을 생성한다.
     *
     * @param expiry    만료 시각
     * @param secretKey HMAC 비밀키 (hex 인코딩, 최소 32바이트 권장)
     * @return 서명된 토큰 문자열
     */
    public static SignedToken sign(Instant expiry, String secretKey) {
        byte[] nonceBytes = new byte[16];
        RANDOM.nextBytes(nonceBytes);
        String nonce = HexFormat.of().formatHex(nonceBytes);

        String payload = nonce + ":" + expiry.getEpochSecond();
        String encodedPayload = URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String hmac = computeHmac(encodedPayload, secretKey);
        String token = encodedPayload + "." + hmac;
        String nonceHash = AccessTokenHasher.hash(nonce);

        return new SignedToken(token, nonceHash);
    }

    /**
     * 서명된 토큰을 검증하고 클레임을 반환한다.
     *
     * @throws InvalidTokenException 서명 불일치, 만료, 형식 오류 시
     */
    public static TokenClaims verify(String token, String secretKey, Instant now) {
        int dotIndex = token.indexOf('.');
        if (dotIndex < 0) {
            throw new InvalidTokenException("잘못된 토큰 형식");
        }
        String encodedPayload = token.substring(0, dotIndex);
        String hmac = token.substring(dotIndex + 1);

        String expectedHmac = computeHmac(encodedPayload, secretKey);
        if (!constantTimeEquals(hmac, expectedHmac)) {
            throw new InvalidTokenException("토큰 서명 불일치");
        }

        String payload = new String(URL_DECODER.decode(encodedPayload), StandardCharsets.UTF_8);
        int colonIndex = payload.indexOf(':');
        if (colonIndex < 0) {
            throw new InvalidTokenException("잘못된 토큰 페이로드");
        }
        String nonce = payload.substring(0, colonIndex);
        long epochSeconds = Long.parseLong(payload.substring(colonIndex + 1));
        Instant expiry = Instant.ofEpochSecond(epochSeconds);

        if (now.isAfter(expiry)) {
            throw new InvalidTokenException("토큰 만료");
        }

        String nonceHash = AccessTokenHasher.hash(nonce);
        return new TokenClaims(nonce, nonceHash, expiry);
    }

    /** 토큰이 서명된 형식인지 (`.` 구분자 포함) 판별한다. */
    public static boolean isSigned(String token) {
        return token != null && token.indexOf('.') > 0;
    }

    private static String computeHmac(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC 계산 실패", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    public record SignedToken(String rawToken, String nonceHash) {}

    public record TokenClaims(String nonce, String nonceHash, Instant expiry) {}
}
