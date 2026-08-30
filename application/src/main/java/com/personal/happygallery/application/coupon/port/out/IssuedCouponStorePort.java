package com.personal.happygallery.application.coupon.port.out;

import com.personal.happygallery.domain.coupon.IssuedCoupon;
import java.util.List;

public interface IssuedCouponStorePort {

    IssuedCoupon save(IssuedCoupon issuedCoupon);

    List<IssuedCoupon> saveAll(Iterable<IssuedCoupon> issuedCoupons);
}
