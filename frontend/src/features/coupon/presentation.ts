import { formatKRW } from "@/shared/lib";

interface CouponDiscount {
  discountType: "FIXED" | "PERCENT";
  discountValue: number;
  maxDiscountAmount: number | null;
}

export function couponDiscountLabel(coupon: CouponDiscount): string {
  if (coupon.discountType === "FIXED") {
    return `${formatKRW(coupon.discountValue)} 할인`;
  }
  return `${coupon.discountValue}% 할인 · 최대 ${formatKRW(coupon.maxDiscountAmount ?? 0)}`;
}
