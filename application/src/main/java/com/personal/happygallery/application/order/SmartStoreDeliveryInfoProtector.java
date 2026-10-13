package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.dto.SmartStoreDeliveryInfo;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class SmartStoreDeliveryInfoProtector {

    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;

    SmartStoreDeliveryInfoProtector(ObjectMapper objectMapper, FieldEncryptor fieldEncryptor) {
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    String encrypt(SmartStoreDeliveryInfo deliveryInfo) {
        return deliveryInfo == null
                ? null
                : fieldEncryptor.encrypt(objectMapper.writeValueAsString(deliveryInfo));
    }

    SmartStoreDeliveryInfo decrypt(String encrypted) {
        return encrypted == null
                ? null
                : objectMapper.readValue(fieldEncryptor.decrypt(encrypted), SmartStoreDeliveryInfo.class);
    }
}
