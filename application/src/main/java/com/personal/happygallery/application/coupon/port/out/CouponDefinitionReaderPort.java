package com.personal.happygallery.application.coupon.port.out;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponDefinitionReaderPort {

    Optional<CouponDefinition> findById(Long id);

    Optional<CouponDefinition> findByIdForUpdate(Long id);

    Optional<CouponDefinition> findByIdForClaim(Long id);

    List<CouponDefinition> findAllById(Iterable<Long> ids);

    List<CouponDefinition> findAllByOrderByIdDesc();

    List<CouponDefinition> findClaimableByUserId(
            Long userId, LocalDateTime now, int limit);
}
