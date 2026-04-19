package com.personal.happygallery.application.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Guest access token 생성·해싱 유틸.
 *
 * <p>생성 시 32자 hex 토큰(UUID v4, 하이픈 제거)을 반환하고,
 * DB에는 SHA-256 해시(64자 hex)만 저장한다.
 * 조회 시 입력 토큰을 해시하여 저장된 값과 비교한다.
 */
public final class AccessTokenHasher {

    private AccessTokenHasher() {}

    /** 32자 hex 토큰 생성 (UUID v4 기반, SecureRandom). */
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /** SHA-256 해시 → 64자 lowercase hex. */
    public static String hash(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
