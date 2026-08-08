package com.personal.happygallery.application.coupon.port.out;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import java.util.List;
import java.util.Optional;

public interface CouponDefinitionReaderPort {

    Optional<CouponDefinition> findById(Long id);

    Optional<CouponDefinition> findByIdForUpdate(Long id);

    List<CouponDefinition> findAllById(Iterable<Long> ids);

    List<CouponDefinition> findAllByOrderByIdDesc();
}
