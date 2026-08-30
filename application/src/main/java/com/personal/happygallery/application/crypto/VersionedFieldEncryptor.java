package com.personal.happygallery.application.crypto;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AES 암호문에 키 ID를 기록하고 이전 키로 만든 암호문도 읽는 필드 암호화기. */
public final class VersionedFieldEncryptor implements FieldEncryptor {

    private static final String PREFIX = "hg:";

    private final String activeKeyId;
    private final Map<String, FieldEncryptor> encryptors;

    public VersionedFieldEncryptor(String activeKeyId, Map<String, byte[]> keys) {
        if (activeKeyId == null || activeKeyId.isBlank()) {
            throw new IllegalArgumentException("활성 암호화 키 ID는 필수입니다.");
        }
        LinkedHashMap<String, FieldEncryptor> configured = new LinkedHashMap<>();
        keys.forEach((keyId, key) -> configured.put(keyId, new SpringSecurityFieldEncryptor(key)));
        if (!configured.containsKey(activeKeyId)) {
            throw new IllegalArgumentException("활성 암호화 키를 키링에서 찾을 수 없습니다: " + activeKeyId);
        }
        this.activeKeyId = activeKeyId;
        this.encryptors = Collections.unmodifiableMap(configured);
    }

    @Override
    public String encrypt(String plaintext) {
        return prefix(activeKeyId) + encryptors.get(activeKeyId).encrypt(plaintext);
    }

    @Override
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            throw new IllegalArgumentException("암호문은 필수입니다.");
        }
        if (encrypted.startsWith(PREFIX)) {
            return decryptVersioned(encrypted);
        }
        return decryptLegacy(encrypted);
    }

    public String reencrypt(String encrypted) {
        String plaintext = decrypt(encrypted);
        if (isEncryptedWithActiveKey(encrypted)) {
            return encrypted;
        }
        return encrypt(plaintext);
    }

    public boolean isEncryptedWithActiveKey(String encrypted) {
        return encrypted != null && encrypted.startsWith(prefix(activeKeyId));
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public List<String> keyIds() {
        return List.copyOf(encryptors.keySet());
    }

    private String decryptVersioned(String encrypted) {
        int keyIdEnd = encrypted.indexOf(':', PREFIX.length());
        if (keyIdEnd < 0 || keyIdEnd == encrypted.length() - 1) {
            throw new IllegalStateException("암호문 키 버전 형식이 올바르지 않습니다.");
        }
        String keyId = encrypted.substring(PREFIX.length(), keyIdEnd);
        FieldEncryptor encryptor = encryptors.get(keyId);
        if (encryptor == null) {
            throw new IllegalStateException("암호문에 필요한 키를 찾을 수 없습니다: " + keyId);
        }
        return encryptor.decrypt(encrypted.substring(keyIdEnd + 1));
    }

    private String decryptLegacy(String encrypted) {
        IllegalStateException lastFailure = null;
        for (FieldEncryptor encryptor : encryptors.values()) {
            try {
                return encryptor.decrypt(encrypted);
            } catch (IllegalStateException e) {
                lastFailure = e;
            }
        }
        throw new IllegalStateException("등록된 키로 레거시 암호문을 복호화할 수 없습니다.", lastFailure);
    }

    private static String prefix(String keyId) {
        return PREFIX + keyId + ':';
    }
}
