package com.personal.happygallery.application.crypto;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

/**
 * Spring Security Crypto의 AES-GCM 구현체를 사용하는 필드 암호화기.
 */
public final class SpringSecurityFieldEncryptor implements FieldEncryptor {

    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;

    private final BytesEncryptor encryptor;

    public SpringSecurityFieldEncryptor(byte[] keyBytes) {
        if (keyBytes.length != AES_256_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "AES-256 키는 32바이트여야 합니다 (현재: " + keyBytes.length + ")");
        }
        this.encryptor = new AesBytesEncryptor(
                new SecretKeySpec(keyBytes.clone(), "AES"),
                KeyGenerators.secureRandom(GCM_IV_BYTES),
                CipherAlgorithm.GCM);
    }

    @Override
    public String encrypt(String plaintext) {
        try {
            byte[] encrypted = encryptor.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("암호화 실패", e);
        }
    }

    @Override
    public String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = encryptor.decrypt(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("복호화 실패", e);
        }
    }
}
