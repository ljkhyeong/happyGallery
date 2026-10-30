import { OrderItemSummary } from "@/features/my/OrderItemSummary";
import { ListMyOrdersPageStatus } from "@/generated/api/customerStore";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Card } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { fetchMyOrdersPage } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyHistoryList } from "@/features/my/useMyHistoryList";
import { MySectionPage } from "@/features/my/MySectionPage";
import { queryKeys } from "@/shared/api";
import { CursorLoadMore, CursorQueryErrorAlert, LoadingSpinner, EmptyState, StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { orderNumberLabel } from "@/shared/lib/labels";

const DEFAULT_SORT = "LATEST";
const ORDER_SORT_OPTIONS = [
  { value: "LATEST", label: "최신 주문순" },
  { value: "OLDEST", label: "오래된 주문순" },
  { value: "AMOUNT_DESC", label: "결제 금액 높은순" },
  { value: "AMOUNT_ASC", label: "결제 금액 낮은순" },
] as const;

export function MyOrdersPage() {
  const { search } = useLocation();
  const { filters, query, items: orders } = useMyHistoryList({
    queryKey: queryKeys.member.orders.history,
    fetchPage: fetchMyOrdersPage,
    statusValues: Object.values(ListMyOrdersPageStatus),
    sortOptions: ORDER_SORT_OPTIONS,
    defaultSort: DEFAULT_SORT,
  });
  const hasLoadedOrders = query.data !== undefined;
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(Object.values(ListMyOrdersPageStatus)),
  ];
  const quickTabs = [
    { value: "ALL", label: "전체" },
    ...buildStatusFilterOptions(["PAID_APPROVAL_PENDING", "SHIPPED", "DELIVERED"], { keepOrder: true }),
  ];

  return (
    <MySectionPage
      path="/my/orders"
      actions={(
        <LinkButton to="/products" variant="outline-dark" size="sm">작품 보러가기</LinkButton>
      )}
    >
      <CursorQueryErrorAlert query={query} />
      <MyListFilterBar
        idPrefix="my-orders"
        searchLabel="주문 번호·작품명 검색"
        searchPlaceholder="예: 123 또는 머그"
        filters={filters}
        statusOptions={statusOptions}
        quickTabs={quickTabs}
        sortOptions={ORDER_SORT_OPTIONS}
        count={orders.length}
        hasMore={query.hasNextPage}
      />
      {query.isLoading && <LoadingSpinner />}
      {hasLoadedOrders && orders.length === 0 && (
        <EmptyState message={filters.hasFilter ? "검색 조건에 맞는 주문 내역이 없습니다." : "주문 내역이 없습니다."} />
      )}
      {orders.length > 0 && orders.map((order) => (
        <Card
          key={order.orderId}
          as={Link}
          to={{ pathname: `/my/orders/${order.orderId}`, search }}
          className="mb-2 text-decoration-none my-list-card"
        >
          <Card.Body className="my-list-row py-3 px-3">
            <div className="my-list-main">
              <div className="fw-semibold small">{orderNumberLabel(order.orderId)}</div>
              <OrderItemSummary items={order.items} />
              <small className="text-muted-soft">
                {order.paidAt ? `결제 ${formatDateTime(order.paidAt)}` : formatDateTime(order.createdAt)}
              </small>
            </div>
            <div className="my-list-side">
              <StatusBadge status={order.status} />
              <span>{formatKRW(order.totalAmount)}</span>
            </div>
          </Card.Body>
        </Card>
      ))}
      <CursorLoadMore query={query} label="주문 더 보기" loadingLabel="주문 불러오는 중..." />
    </MySectionPage>
  );
}
