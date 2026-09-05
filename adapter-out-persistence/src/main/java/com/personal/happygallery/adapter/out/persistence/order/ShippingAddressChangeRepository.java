package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.ShippingAddressChangePort;
import com.personal.happygallery.domain.order.ShippingAddressChange;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingAddressChangeRepository extends JpaRepository<ShippingAddressChange, Long>,
        ShippingAddressChangePort {
    @Override
    <S extends ShippingAddressChange> S save(S change);
}
