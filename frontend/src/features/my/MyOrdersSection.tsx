import { Link } from "react-router";
import { OrderItemSummary } from "@/features/my/OrderItemSummary";
import { EmptyState, ErrorAlert, LoadingSpinner, StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { MyOrderSummary } from "./api";

interface Props {
  previewSize?: number;
  orders: MyOrderSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
}

export function MyOrdersSection({ orders, isLoading, error, isFetching, onRetry, previewSize = 3 }: Props) {
  return (
    <section id="my-orders" className="my-recent" aria-labelledby="my-orders-title">
      <div className="my-recent-head">
        <h2 id="my-orders-title">최근 주문</h2>
        <Link to="/my/orders">전체 보기 →</Link>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} onRetry={onRetry} retrying={isFetching} />
      {orders && orders.length === 0 && <EmptyState message="주문 내역이 없습니다." />}
      {orders?.slice(0, previewSize).map((order) => (
        <Link key={order.orderId} to={`/my/orders/${order.orderId}`} className="my-list-card my-recent-item">
          <div className="my-recent-main">
            <strong>주문 #{order.orderId}</strong>
            <OrderItemSummary items={order.items} />
            <small>{order.paidAt ? `결제 ${formatDateTime(order.paidAt)}` : formatDateTime(order.createdAt)}</small>
          </div>
          <div className="my-recent-side">
            <StatusBadge status={order.status} />
            <span>{formatKRW(order.totalAmount)}</span>
          </div>
        </Link>
      ))}
    </section>
  );
}
