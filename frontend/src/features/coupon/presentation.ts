import { periodEndLabel } from "@/features/event/time";
import { formatKRW } from "@/shared/lib";

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

/**
 * 예: `10,000원 이상 · 2026. 11. 30.까지`. 최소 금액이 없으면 기한만 보여 준다.
 * 자정에 끝나는 기한은 같은 화면의 이벤트 기간(`~ 2026. 11. 30.`)과 같은 규칙으로 전날 날짜까지로 쓴다.
 */
export function couponConditionLabel(coupon: CouponCondition): string {
  const minimum = coupon.minOrderAmount > 0 ? `${formatKRW(coupon.minOrderAmount)} 이상 · ` : "";
  return `${minimum}${periodEndLabel(coupon.validUntil)}까지`;
}
