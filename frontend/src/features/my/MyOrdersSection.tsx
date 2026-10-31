import { OrderItemSummary } from "@/features/my/OrderItemSummary";
import { StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { orderNumberLabel } from "@/shared/lib/labels";
import type { MyOrderSummary } from "./api";
import { MyRecentSection } from "./MyRecentSection";

interface Props {
  orders: MyOrderSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
}

export function MyOrdersSection({ orders, ...state }: Props) {
  return (
    <MyRecentSection
      id="my-orders"
      title="최근 주문"
      listTo="/my/orders"
      emptyMessage="주문 내역이 없습니다."
      items={orders}
      {...state}
      itemKey={(order) => order.orderId}
      itemTo={(order) => `/my/orders/${order.orderId}`}
      renderMain={(order) => (
        <>
          <strong>{orderNumberLabel(order.orderId)}</strong>
          <OrderItemSummary items={order.items} />
          <small>{order.paidAt ? `결제 ${formatDateTime(order.paidAt)}` : formatDateTime(order.createdAt)}</small>
        </>
      )}
      renderSide={(order) => (
        <>
          <StatusBadge status={order.status} />
          <span>{formatKRW(order.totalAmount)}</span>
        </>
      )}
    />
  );
}
