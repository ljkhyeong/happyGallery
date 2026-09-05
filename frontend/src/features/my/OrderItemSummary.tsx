import type { MyOrderSummary } from "@/generated/api/customerStore";

export function OrderItemSummary({ items }: Pick<MyOrderSummary, "items">) {
  return <>{items?.map((item) => (
    <div key={item.orderItemId} className="small mt-2">
      <div>{item.productName} · {item.quantity}개</div>
      {item.options.length > 0 && <div className="text-muted-soft">{item.options.join(" / ")}</div>}
    </div>
  ))}</>;
}
