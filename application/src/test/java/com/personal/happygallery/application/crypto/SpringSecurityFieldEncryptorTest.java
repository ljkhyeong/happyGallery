package com.personal.happygallery.application.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpringSecurityFieldEncryptorTest {

    private static final byte[] KEY = HexFormat.of().parseHex(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");

    @DisplayName("암호화 결과는 복호화하면 원문으로 돌아온다")
    @Test
    void encryptAndDecrypt_returnsPlaintext() {
        FieldEncryptor encryptor = new SpringSecurityFieldEncryptor(KEY);

        String encrypted = encryptor.encrypt("01012345678");

        assertThat(encryptor.decrypt(encrypted)).isEqualTo("01012345678");
    }

    @DisplayName("Spring Security 구현체는 기존 AES-GCM 암호문 포맷을 복호화한다")
    @Test
    void decrypt_supportsLegacyAesGcmFormat() throws Exception {
        FieldEncryptor encryptor = new SpringSecurityFieldEncryptor(KEY);
        byte[] iv = HexFormat.of().parseHex("000102030405060708090a0b");
        String legacyEncrypted = legacyEncrypt("01012345678", iv);

        assertThat(encryptor.decrypt(legacyEncrypted)).isEqualTo("01012345678");
    }

    private static String legacyEncrypt(String plaintext, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), new GCMParameterSpec(128, iv));
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = ByteBuffer.allocate(iv.length + ciphertext.length)
                .put(iv)
                .put(ciphertext)
                .array();
        return Base64.getEncoder().encodeToString(encrypted);
    }
}
