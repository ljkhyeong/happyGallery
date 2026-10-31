import type { FulfillmentType } from "@/features/payment";
import { formatKRW } from "@/shared/lib";

/** 수령 방법에 따른 배송비. 매장 수령은 0원, 택배는 주문 정책의 배송비이고, 고르기 전이거나 정책을 아직 모르면 null이다. */
export function orderShippingFee(
  fulfillmentType: FulfillmentType | null,
  policyShippingFee: number | undefined,
): number | null {
  if (fulfillmentType === "PICKUP") return 0;
  if (fulfillmentType === "SHIPPING" && policyShippingFee !== undefined) return policyShippingFee;
  return null;
}

/**
 * 결제 예정 금액 표기. 요약(OrderPriceSummary)과 모바일 하단 결제 칸이 같은 문구를 쓴다.
 * 배송비를 아직 모르면 아는 금액까지만 보이고 배송비가 더해진다는 것을 밝힌다.
 */
export function orderPaymentTotalLabel(amountAfterDiscounts: number, shippingFee: number | null): string {
  const amount = Math.max(0, amountAfterDiscounts);
  return shippingFee === null ? `${formatKRW(amount)} + 배송비` : formatKRW(amount + shippingFee);
}
