import type { FulfillmentType } from "@/features/payment";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { formatKRW } from "@/shared/lib";

interface Props {
  itemAmount: number;
  fulfillmentType: FulfillmentType | null;
  couponDiscountAmount?: number;
  rewardAmount?: number;
  className?: string;
}

export function OrderPriceSummary({
  itemAmount,
  fulfillmentType,
  couponDiscountAmount = 0,
  rewardAmount = 0,
  className,
}: Props) {
  const { data: pricePolicy, isLoading, isError } = useOrderPricePolicy();
  let shippingFee: number | null = null;
  if (fulfillmentType === "PICKUP") {
    shippingFee = 0;
  } else if (fulfillmentType === "SHIPPING" && pricePolicy) {
    shippingFee = pricePolicy.shippingFee;
  }

  let shippingFeeLabel = "수령 방법 선택 후 확정";
  if (fulfillmentType === "PICKUP" || shippingFee === 0) {
    shippingFeeLabel = "무료";
  } else if (fulfillmentType === "SHIPPING" && isLoading) {
    shippingFeeLabel = "확인 중";
  } else if (fulfillmentType === "SHIPPING" && (isError || shippingFee == null)) {
    shippingFeeLabel = "조회 실패";
  } else if (shippingFee != null) {
    shippingFeeLabel = formatKRW(shippingFee);
  }

  return (
    <div className={className}>
      <div className="d-flex justify-content-between align-items-center mb-2">
        <span className="text-muted-soft">상품 합계</span>
        <span>{formatKRW(itemAmount)}</span>
      </div>
      {couponDiscountAmount > 0 && (
        <div className="d-flex justify-content-between align-items-center mb-2">
          <span className="text-muted-soft">쿠폰 할인</span>
          <span>-{formatKRW(couponDiscountAmount)}</span>
        </div>
      )}
      {rewardAmount > 0 && (
        <div className="d-flex justify-content-between align-items-center mb-2">
          <span className="text-muted-soft">적립금 사용</span>
          <span>-{formatKRW(rewardAmount)}</span>
        </div>
      )}
      <div className="d-flex justify-content-between align-items-center mb-2">
        <span className="text-muted-soft">배송비</span>
        <span>{shippingFeeLabel}</span>
      </div>
      <div className="d-flex justify-content-between align-items-center pt-3 mt-2 border-top">
        <span className="fw-semibold">결제 예정 금액</span>
        <span className="fs-5 fw-bold">
          {shippingFee == null
            ? "-"
            : formatKRW(Math.max(0, itemAmount - couponDiscountAmount - rewardAmount) + shippingFee)}
        </span>
      </div>
    </div>
  );
}
