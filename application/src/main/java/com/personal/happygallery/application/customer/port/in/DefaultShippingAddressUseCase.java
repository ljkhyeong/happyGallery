package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.order.ShippingAddress;

public interface DefaultShippingAddressUseCase {
    record View(long version, ShippingAddress shippingAddress) {}
    View get(Long userId);
    void save(Long userId, long version, ShippingAddress address);
    void delete(Long userId, long version);
}
