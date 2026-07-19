package com.personal.happygallery.application.order;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.util.Objects;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 배송지 스냅샷을 구조화된 JSON으로 직렬화한 뒤 암호화한다. */
@Component
public class ShippingAddressProtector {

    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;

    public ShippingAddressProtector(ObjectMapper objectMapper, FieldEncryptor fieldEncryptor) {
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    public String encrypt(ShippingAddress address) {
        return fieldEncryptor.encrypt(objectMapper.writeValueAsString(
                Objects.requireNonNull(address, "address must not be null")));
    }

    public ShippingAddress decrypt(String encrypted) {
        return objectMapper.readValue(fieldEncryptor.decrypt(encrypted), ShippingAddress.class);
    }
}
