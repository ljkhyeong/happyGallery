package com.personal.happygallery.domain.coupon;

/** 회원에게 발급된 쿠폰의 사용 상태. */
public enum IssuedCouponStatus {
    AVAILABLE,
    RESERVED,
    REDEEMED,
    EXPIRED,
    CANCELED
}
