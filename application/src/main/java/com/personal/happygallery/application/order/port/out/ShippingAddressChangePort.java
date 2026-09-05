package com.personal.happygallery.application.order.port.out;

import com.personal.happygallery.domain.order.ShippingAddressChange;

public interface ShippingAddressChangePort {
    <S extends ShippingAddressChange> S save(S change);
}
