package com.personal.happygallery.domain.crypto;

/**
 * 복호화가 필요한 개인정보 필드의 양방향 암호화 계약.
 */
public interface FieldEncryptor {

    String encrypt(String plaintext);

    String decrypt(String encrypted);

    default String decryptNullable(String encrypted) {
        return encrypted == null ? null : decrypt(encrypted);
    }
}
