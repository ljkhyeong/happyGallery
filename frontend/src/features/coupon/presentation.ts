import { formatDateTime, formatKRW } from "@/shared/lib";

interface CouponDiscount {
  discountType: "FIXED" | "PERCENT";
  discountValue: number;
  maxDiscountAmount: number | null;
}

interface CouponCondition {
  minOrderAmount: number;
  validUntil: string;
}

export function couponDiscountLabel(coupon: CouponDiscount): string {
  if (coupon.discountType === "FIXED") {
    return `${formatKRW(coupon.discountValue)} 할인`;
  }
  return `${coupon.discountValue}% 할인 · 최대 ${formatKRW(coupon.maxDiscountAmount ?? 0)}`;
}

/** 예: `₩10,000 이상 · 2026. 12. 31. 오후 11:59까지`. 최소 금액이 없으면 기한만 보여 준다. */
export function couponConditionLabel(coupon: CouponCondition): string {
  const minimum = coupon.minOrderAmount > 0 ? `${formatKRW(coupon.minOrderAmount)} 이상 · ` : "";
  return `${minimum}${formatDateTime(coupon.validUntil)}까지`;
}
