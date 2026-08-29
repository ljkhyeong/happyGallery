package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderProvider.DeliveryInfo;
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

    String encrypt(DeliveryInfo deliveryInfo) {
        return deliveryInfo == null
                ? null
                : fieldEncryptor.encrypt(objectMapper.writeValueAsString(deliveryInfo));
    }

    DeliveryInfo decrypt(String encrypted) {
        return encrypted == null
                ? null
                : objectMapper.readValue(fieldEncryptor.decrypt(encrypted), DeliveryInfo.class);
    }
}
