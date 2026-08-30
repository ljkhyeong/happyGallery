package com.personal.happygallery.application.coupon.port.in;

import com.personal.happygallery.domain.coupon.CouponDiscountType;
import java.time.LocalDateTime;

/** 관리자 쿠폰 정의 생성·수정 입력. */
public record CouponDefinitionCommand(
        String name,
        CouponDiscountType discountType,
        long discountValue,
        long minOrderAmount,
        Long maxDiscountAmount,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        boolean active,
        boolean publiclyClaimable
) {}
