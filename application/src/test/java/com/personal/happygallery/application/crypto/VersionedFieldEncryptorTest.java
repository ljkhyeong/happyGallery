package com.personal.happygallery.application.crypto;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class VersionedFieldEncryptorTest {

    private static final byte[] ACTIVE_KEY = filledKey((byte) 1);
    private static final byte[] PREVIOUS_KEY = filledKey((byte) 2);

    @DisplayName("신규 암호문은 활성 키 ID를 기록하고 이전 키의 암호문을 활성 키로 재암호화한다")
    @Test
    void encryptAndReencrypt_useActiveKeyVersion() {
        VersionedFieldEncryptor encryptor = new VersionedFieldEncryptor("v2", keyRing());
        String legacyCiphertext = new SpringSecurityFieldEncryptor(PREVIOUS_KEY).encrypt("개인정보");

        String activeCiphertext = encryptor.encrypt("신규 개인정보");
        String reencrypted = encryptor.reencrypt(legacyCiphertext);

        assertSoftly(softly -> {
            softly.assertThat(activeCiphertext).startsWith("hg:v2:");
            softly.assertThat(encryptor.decrypt(activeCiphertext)).isEqualTo("신규 개인정보");
            softly.assertThat(reencrypted).startsWith("hg:v2:");
            softly.assertThat(encryptor.decrypt(reencrypted)).isEqualTo("개인정보");
        });
    }

    @DisplayName("키링에 없는 키 ID가 기록된 암호문은 복호화를 거부한다")
    @Test
    void decrypt_unknownKeyVersion_rejected() {
        VersionedFieldEncryptor encryptor = new VersionedFieldEncryptor("v2", keyRing());

        assertThatThrownBy(() -> encryptor.decrypt("hg:v3:ciphertext"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("키를 찾을 수 없습니다");
        assertThat(encryptor.keyIds()).containsExactly("v2", "v1");
    }

    private static Map<String, byte[]> keyRing() {
        LinkedHashMap<String, byte[]> keys = new LinkedHashMap<>();
        keys.put("v2", ACTIVE_KEY);
        keys.put("v1", PREVIOUS_KEY);
        return keys;
    }

    private static byte[] filledKey(byte value) {
        byte[] key = new byte[32];
        Arrays.fill(key, value);
        return key;
    }
}
