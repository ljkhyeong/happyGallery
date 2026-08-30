package com.personal.happygallery.application.coupon.port.in;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import java.util.List;

/** 회원의 공개 쿠폰 발급과 보유 쿠폰 조회 유스케이스. */
public interface CouponMemberUseCase {

    IssuedCouponView claim(Long userId, Long definitionId);

    List<CouponDefinition> listClaimableCoupons(Long userId);

    List<IssuedCouponView> listMyCoupons(Long userId);

    record IssuedCouponView(IssuedCoupon issuedCoupon, CouponDefinition definition) {}
}
