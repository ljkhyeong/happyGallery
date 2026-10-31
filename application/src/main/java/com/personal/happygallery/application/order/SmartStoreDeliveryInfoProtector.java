package com.personal.happygallery.application.order;

import com.personal.happygallery.application.crypto.EncryptedJsonCodec;
import com.personal.happygallery.application.order.dto.SmartStoreDeliveryInfo;
import org.springframework.stereotype.Component;

@Component
class SmartStoreDeliveryInfoProtector {

    private final EncryptedJsonCodec codec;

    SmartStoreDeliveryInfoProtector(EncryptedJsonCodec codec) {
        this.codec = codec;
    }

    String encrypt(SmartStoreDeliveryInfo deliveryInfo) {
        return deliveryInfo == null ? null : codec.encrypt(deliveryInfo);
    }

    SmartStoreDeliveryInfo decrypt(String encrypted) {
        return encrypted == null ? null : codec.decrypt(encrypted, SmartStoreDeliveryInfo.class);
    }
}
