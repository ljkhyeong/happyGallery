import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { formatKRW } from "@/shared/lib";
import type { ProductType } from "@/shared/types/product";

/** 작품 상세의 배송·교환 안내. 제작(준비)·수령·배송비·교환반품·적립 규칙을 주문 정책과 같은 내용으로 보여 준다. */
export function ProductPurchaseGuide({ type }: { type: ProductType }) {
  const { data: pricePolicy } = useOrderPricePolicy();
  const madeToOrder = type === "MADE_TO_ORDER";
  const rows = [
    madeToOrder
      ? { label: "제작", value: "공방이 주문을 승인한 뒤 안내된 기간과 사양대로 만듭니다." }
      : { label: "준비", value: "공방이 주문을 승인한 뒤 고른 수령 방법에 맞춰 준비합니다." },
    {
      label: "수령 방법",
      value: "택배 또는 매장 수령 중에서 고릅니다. 매장 수령은 준비 완료 알림을 받은 뒤 안내된 기한 안에 찾아가면 됩니다.",
    },
    {
      label: "배송비",
      value: !pricePolicy
        ? "매장 수령은 무료이고, 택배 배송비는 주문서에서 확인할 수 있습니다."
        : pricePolicy.shippingFee === 0
          ? "매장 수령·택배 모두 무료입니다."
          : `매장 수령은 무료, 택배는 주문당 ${formatKRW(pricePolicy.shippingFee)}입니다.`,
    },
    {
      label: "교환·반품",
      value: madeToOrder
        ? "받은 뒤 주문 상세에서 반품·교환을 요청하면 공방이 확인해 처리합니다. 주문 제작 작품은 결제 전에 동의한 제작 조건에 따라 단순 변심 반품이 어려울 수 있습니다."
        : "받은 뒤 주문 상세에서 하자·오배송·단순 변심 사유로 반품·교환을 요청하면 공방이 확인해 처리합니다.",
    },
    {
      label: "적립",
      value: "회원은 작품을 받은 뒤 결제한 상품 금액의 1%를 적립금으로 받습니다. 적립금은 1년 동안 쓸 수 있습니다.",
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
