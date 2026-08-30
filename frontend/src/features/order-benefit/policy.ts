export type CouponDiscountType = "FIXED" | "PERCENT";

export interface CouponDiscountPolicy {
  discountType: CouponDiscountType;
  discountValue: number;
  minOrderAmount: number;
  maxDiscountAmount: number | null;
}

function nonNegativeInteger(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.min(Number.MAX_SAFE_INTEGER, Math.max(0, Math.floor(value)));
}

function calculatePercentDiscount(productAmount: number, discountValue: number): number {
  const product = BigInt(productAmount);
  const calculated = product * BigInt(discountValue) / 100n;
  return Number(calculated > product ? product : calculated);
}

export function calculateCouponDiscount(
  coupon: CouponDiscountPolicy | null | undefined,
  productAmount: number,
): number {
  if (!coupon) return 0;

  const normalizedProductAmount = nonNegativeInteger(productAmount);
  if (normalizedProductAmount < nonNegativeInteger(coupon.minOrderAmount)) return 0;

  const discountValue = nonNegativeInteger(coupon.discountValue);
  const calculated = coupon.discountType === "FIXED"
    ? discountValue
    : calculatePercentDiscount(normalizedProductAmount, discountValue);
  const capped = coupon.discountType === "PERCENT" && coupon.maxDiscountAmount !== null
    ? Math.min(calculated, nonNegativeInteger(coupon.maxDiscountAmount))
    : calculated;

  return Math.min(normalizedProductAmount, capped);
}

export function maximumRewardPoints(
  availableBalance: number,
  productAmount: number,
  couponDiscountAmount: number,
): number {
  const payableProductAmount = Math.max(
    0,
    nonNegativeInteger(productAmount) - nonNegativeInteger(couponDiscountAmount),
  );
  return Math.min(nonNegativeInteger(availableBalance), payableProductAmount);
}

export function normalizeRewardPoints(value: number, maximum: number): number {
  return Math.min(nonNegativeInteger(value), nonNegativeInteger(maximum));
}
