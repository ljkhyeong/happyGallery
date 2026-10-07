package com.personal.happygallery.application.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.util.HexFormat;
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
}
