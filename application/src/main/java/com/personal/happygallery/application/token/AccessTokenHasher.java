package com.personal.happygallery.application.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Guest access token 해싱 유틸.
 *
 * <p>DB에는 SHA-256 해시(64자 hex)만 저장하며,
 * 조회 시 입력 토큰을 해시하여 저장된 값과 비교한다.
 */
public final class AccessTokenHasher {

    private AccessTokenHasher() {}

    /** SHA-256 해시 → 64자 lowercase hex. */
    public static String hash(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
