package com.personal.happygallery.domain.crypto;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * HMAC-SHA256 블라인드 인덱스 생성기.
 *
 * <p>암호화된 개인정보의 동등 검색을 위한 단방향 인덱스를 생성한다.
 * {@code WHERE phone_hmac = ?} 형태로 사용한다.
 */
public final class BlindIndexer {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKey secretKey;

    public BlindIndexer(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("HMAC 키는 32바이트여야 합니다 (현재: " + keyBytes.length + ")");
        }
        this.secretKey = new SecretKeySpec(keyBytes.clone(), ALGORITHM);
    }

    /** 평문 → 64자 lowercase hex HMAC */
    public String index(String plaintext) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 생성 실패", e);
        }
    }
}
