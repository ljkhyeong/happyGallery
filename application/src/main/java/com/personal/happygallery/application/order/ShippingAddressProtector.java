package com.personal.happygallery.application.order;

import com.personal.happygallery.application.crypto.EncryptedJsonCodec;
import com.personal.happygallery.domain.order.ShippingAddress;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** 배송지 스냅샷을 구조화된 JSON으로 직렬화한 뒤 암호화한다. */
@Component
public class ShippingAddressProtector {

    private final EncryptedJsonCodec codec;

    public ShippingAddressProtector(EncryptedJsonCodec codec) {
        this.codec = codec;
    }

    public String encrypt(ShippingAddress address) {
        return codec.encrypt(Objects.requireNonNull(address, "address must not be null"));
    }

    public ShippingAddress decrypt(String encrypted) {
        return codec.decrypt(encrypted, ShippingAddress.class);
    }
}
