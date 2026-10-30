import { useQuery } from "@tanstack/react-query";
import { myCouponsQuery } from "@/features/coupon/queries";
import { orderPaymentTotalLabel, orderShippingFee } from "@/features/order/orderPaymentTotal";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import type { FulfillmentType } from "@/features/payment";
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
  // 배송비는 택배를 고른 경우에만 필요하다. 그때는 수령 방법 칸이 이미 같은 캐시로 정책을 불러 둔다.
  const { data: pricePolicy } = useOrderPricePolicy(fulfillmentType === "SHIPPING");
  const coupon = couponId === null
    ? undefined
    : selectableCoupons(couponsQuery.data, productAmount).find((item) => item.id === couponId);
  const amount = productAmount - calculateCouponDiscount(coupon, productAmount) - rewardAmount;
  return {
    label: "결제 예정 금액",
    amount: orderPaymentTotalLabel(amount, orderShippingFee(fulfillmentType, pricePolicy?.shippingFee)),
  };
}
