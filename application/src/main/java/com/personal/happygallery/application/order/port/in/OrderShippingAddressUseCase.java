package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.domain.order.ShippingAddress;

public interface OrderShippingAddressUseCase {
    void updateMember(Long orderId, Long userId, long version, ShippingAddress address);
    void updateGuest(Long orderId, String accessToken, long version, ShippingAddress address);
}
