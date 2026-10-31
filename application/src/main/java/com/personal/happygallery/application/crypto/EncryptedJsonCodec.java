package com.personal.happygallery.application.crypto;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 값을 JSON으로 직렬화한 뒤 필드 암호화하고, 복호화한 JSON을 다시 객체로 읽는다. */
@Component
public class EncryptedJsonCodec {

    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;

    public EncryptedJsonCodec(ObjectMapper objectMapper, FieldEncryptor fieldEncryptor) {
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    public String encrypt(Object value) {
        return fieldEncryptor.encrypt(objectMapper.writeValueAsString(value));
    }

    public <T> T decrypt(String encrypted, Class<T> type) {
        return objectMapper.readValue(fieldEncryptor.decrypt(encrypted), type);
    }
}
