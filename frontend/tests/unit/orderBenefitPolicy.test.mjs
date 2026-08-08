import assert from "node:assert/strict";
import test from "node:test";

import {
  calculateCouponDiscount,
  maximumRewardPoints,
  normalizeRewardPoints,
} from "../../src/features/order-benefit/policy.ts";

test("정액 쿠폰은 상품 금액을 넘지 않고 최소 주문 금액을 지킨다", () => {
  const coupon = {
    discountType: "FIXED",
    discountValue: 5000,
    minOrderAmount: 10000,
    maxDiscountAmount: null,
  };

  assert.equal(calculateCouponDiscount(coupon, 9000), 0);
  assert.equal(calculateCouponDiscount(coupon, 12000), 5000);
  assert.equal(calculateCouponDiscount({ ...coupon, discountValue: 20000 }, 12000), 12000);
});

test("정률 쿠폰은 원 단위로 내림하고 최대 할인 금액을 적용한다", () => {
  const coupon = {
    discountType: "PERCENT",
    discountValue: 15,
    minOrderAmount: 0,
    maxDiscountAmount: 3000,
  };

  assert.equal(calculateCouponDiscount(coupon, 9999), 1499);
  assert.equal(calculateCouponDiscount(coupon, 30000), 3000);
});

test("정률 쿠폰은 안전한 정수 경계에서도 중간곱 오차 없이 계산한다", () => {
  const maximumSafeAmount = Number.MAX_SAFE_INTEGER;
  const expected = Number(BigInt(maximumSafeAmount) * 15n / 100n);
  const coupon = {
    discountType: "PERCENT",
    discountValue: 15,
    minOrderAmount: 0,
    maxDiscountAmount: null,
  };

  assert.equal(calculateCouponDiscount(coupon, maximumSafeAmount), expected);
  assert.equal(
    calculateCouponDiscount({ ...coupon, discountValue: 100 }, Number.MAX_VALUE),
    maximumSafeAmount,
  );
});

test("적립금 사용 한도는 잔액과 쿠폰 적용 후 상품 금액 중 작은 값이다", () => {
  assert.equal(maximumRewardPoints(12000, 30000, 5000), 12000);
  assert.equal(maximumRewardPoints(50000, 30000, 5000), 25000);
  assert.equal(maximumRewardPoints(1000, 1000, 1000), 0);
});

test("적립금 입력은 음수와 소수, 한도 초과를 안전하게 정규화한다", () => {
  assert.equal(normalizeRewardPoints(-1, 1000), 0);
  assert.equal(normalizeRewardPoints(123.9, 1000), 123);
  assert.equal(normalizeRewardPoints(1500, 1000), 1000);
  assert.equal(normalizeRewardPoints(Number.NaN, 1000), 0);
});
