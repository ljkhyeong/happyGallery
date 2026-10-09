import type { ClaimableCouponResponse } from "./api";
import { couponConditionLabel, couponDiscountLabel } from "./presentation";

type CouponSummaryValue = Pick<
  ClaimableCouponResponse,
  "name" | "discountType" | "discountValue" | "maxDiscountAmount" | "minOrderAmount" | "validUntil"
>;

/** 쿠폰 이름·할인·사용 조건을 세 줄로 보여 준다. */
export function CouponSummary({ coupon }: { coupon: CouponSummaryValue }) {
  return (
    <div>
      <strong className="d-block mb-1">{coupon.name}</strong>
      <div>{couponDiscountLabel(coupon)}</div>
      <small className="text-muted-soft">{couponConditionLabel(coupon)}</small>
    </div>
  );
}
