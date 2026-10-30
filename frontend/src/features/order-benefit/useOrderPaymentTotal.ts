import { useQuery } from "@tanstack/react-query";
import { myCouponsQuery } from "@/features/coupon/queries";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import type { FulfillmentType } from "@/features/payment";
import { formatKRW } from "@/shared/lib";
import { calculateCouponDiscount, selectableCoupons } from "./policy";

interface Params {
  productAmount: number;
  fulfillmentType: FulfillmentType | null;
  /** 회원 주문에서 고른 쿠폰. 고르지 않았거나 비회원이면 null이다. */
  couponId: number | null;
  rewardAmount: number;
}

/**
 * 모바일 하단 결제 칸에 보일 결제 예정 금액. 요약의 `OrderPriceSummary`와 같은 규칙으로 계산한다.
 * 수령 방법을 고르기 전이거나 배송비를 아직 모르면 아는 금액에 배송비가 더해진다고 밝힌다.
 */
export function useOrderPaymentTotal({ productAmount, fulfillmentType, couponId, rewardAmount }: Params) {
  // 쿠폰 목록은 MemberOrderBenefits와 같은 캐시를 쓰고, 쿠폰을 고르지 않았으면 조회하지 않는다.
  const couponsQuery = useQuery({ ...myCouponsQuery(), enabled: couponId !== null });
  const { data: pricePolicy } = useOrderPricePolicy();
  const coupon = couponId === null
    ? undefined
    : selectableCoupons(couponsQuery.data, productAmount).find((item) => item.id === couponId);
  const amount = Math.max(0, productAmount - calculateCouponDiscount(coupon, productAmount) - rewardAmount);
  let shippingFee: number | null = null;
  if (fulfillmentType === "PICKUP") {
    shippingFee = 0;
  } else if (fulfillmentType === "SHIPPING" && pricePolicy) {
    shippingFee = pricePolicy.shippingFee;
  }
  return {
    label: "결제 예정 금액",
    amount: shippingFee === null ? `${formatKRW(amount)} + 배송비` : formatKRW(amount + shippingFee),
  };
}
