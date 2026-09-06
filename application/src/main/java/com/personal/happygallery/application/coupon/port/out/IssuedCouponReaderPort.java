package com.personal.happygallery.application.coupon.port.out;

import com.personal.happygallery.domain.coupon.IssuedCoupon;
import java.util.List;
import java.util.Optional;

public interface IssuedCouponReaderPort {

    Optional<IssuedCoupon> findByIdForUpdate(Long id);

    boolean existsByUserIdAndDefinitionId(Long userId, Long definitionId);

    boolean existsByDefinitionId(Long definitionId);

    List<IssuedCoupon> findTop100ByUserIdOrderByClaimedAtDescIdDesc(Long userId);
}
