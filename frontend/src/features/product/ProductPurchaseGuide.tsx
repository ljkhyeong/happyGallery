import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { formatKRW, PRODUCT_FULFILLMENT_LABEL } from "@/shared/lib";
import type { ProductType } from "@/shared/types/product";

/** 작품 상세의 구매 안내. 수령·배송비·교환반품·적립 규칙을 주문 정책과 같은 문장으로 보여 준다. */
export function ProductPurchaseGuide({ type }: { type: ProductType }) {
  const { data: pricePolicy } = useOrderPricePolicy();
  const rows = [
    {
      label: "수령 방법",
      value: `${PRODUCT_FULFILLMENT_LABEL[type] ?? ""} 매장 수령은 준비 완료 알림을 받은 뒤 안내된 기한 안에 공방에서 받아 갑니다.`,
    },
    {
      label: "배송비",
      value: !pricePolicy
        ? "매장 수령 무료 · 택배 배송비는 주문서에서 확인할 수 있습니다."
        : pricePolicy.shippingFee === 0
          ? "매장 수령·택배 배송 모두 무료"
          : `매장 수령 무료 · 택배 배송 주문당 ${formatKRW(pricePolicy.shippingFee)}`,
    },
    {
      label: "교환·반품",
      value: type === "MADE_TO_ORDER"
        ? "배송·수령을 마친 뒤 주문 상세에서 환불·교환을 요청할 수 있으며 공방 확인 후 처리합니다. 주문제작 작품은 결제 전 동의한 제작 조건에 따라 단순 변심 반품이 제한될 수 있습니다."
        : "배송·수령을 마친 뒤 주문 상세에서 하자·오배송·단순 변심 사유로 환불·교환을 요청할 수 있으며 공방 확인 후 처리합니다.",
    },
    {
      label: "적립",
      value: "회원은 배송·수령이 끝나면 결제한 상품 금액의 1%를 적립금으로 받아 1년간 사용할 수 있습니다.",
    },
  ];

  return (
    <dl className="store-detail-policy">
      {rows.map((row) => (
        <div key={row.label}>
          <dt>{row.label}</dt>
          <dd>{row.value}</dd>
        </div>
      ))}
    </dl>
  );
}
