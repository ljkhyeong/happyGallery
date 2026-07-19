package com.personal.happygallery.bootstrap.config;

import com.personal.happygallery.application.crypto.VersionedFieldEncryptor;
import com.personal.happygallery.bootstrap.config.properties.FieldEncryptionProperties;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CryptoConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public VersionedFieldEncryptor fieldEncryptor(FieldEncryptionProperties props) {
        return new VersionedFieldEncryptor(
                props.activeKeyId(), parseKeys(props.activeKeyId(), props.encryptKey(),
                        props.previousEncryptKeys(), "암호화"));
    }

    @Bean
    public BlindIndexKeyRing blindIndexKeyRing(FieldEncryptionProperties props) {
        return new BlindIndexKeyRing(
                props.activeKeyId(), parseKeys(props.activeKeyId(), props.hmacKey(),
                        props.previousHmacKeys(), "HMAC"));
    }

    @Bean
    public BlindIndexer blindIndexer(FieldEncryptionProperties props) {
        return new BlindIndexer(HexFormat.of().parseHex(props.hmacKey()));
    }

    private static Map<String, byte[]> parseKeys(String activeKeyId,
                                                  String activeKey,
                                                  String previousKeys,
                                                  String label) {
        LinkedHashMap<String, byte[]> keys = new LinkedHashMap<>();
        keys.put(activeKeyId, parseKey(activeKey, label));
        if (previousKeys == null || previousKeys.isBlank()) {
            return keys;
        }
        for (String entry : previousKeys.split(",")) {
            String[] parts = entry.trim().split("=", 2);
            if (parts.length != 2 || !parts[0].matches("^[A-Za-z0-9_-]{1,32}$")) {
                throw new IllegalArgumentException(label + " 이전 키 형식은 keyId=64자리hex 이어야 합니다.");
            }
            if (parts[0].equals(activeKeyId) || keys.putIfAbsent(parts[0], parseKey(parts[1], label)) != null) {
                throw new IllegalArgumentException(label + " 키 ID가 중복되었습니다: " + parts[0]);
            }
        }
        return keys;
    }

    private static byte[] parseKey(String key, String label) {
        if (key == null || !key.matches("^[A-Fa-f0-9]{64}$")) {
            throw new IllegalArgumentException(label + " 키는 64자리 hex여야 합니다.");
        }
        return HexFormat.of().parseHex(key);
    }
}
