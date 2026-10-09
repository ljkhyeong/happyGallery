import { OrderItemSummary } from "@/features/my/OrderItemSummary";
import { ListMyOrdersPageStatus } from "@/generated/api/customerStore";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Card, Col, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { fetchMyOrdersPage } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyHistoryList } from "@/features/my/useMyHistoryList";
import { MySectionPage } from "@/features/my/MySectionPage";
import { queryKeys } from "@/shared/api";
import { CursorLoadMore, LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

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
    ...buildStatusFilterOptions(["PAID_APPROVAL_PENDING", "SHIPPED", "DELIVERED"]),
  ];

  return (
    <MySectionPage
      path="/my/orders"
      description="주문 상태와 결제 금액을 확인하고 주문을 검색하세요."
      actions={(
        <LinkButton to="/products" variant="outline-dark" size="sm">작품 보러가기</LinkButton>
      )}
    >
      <ErrorAlert
        error={query.isFetchNextPageError ? null : query.error}
        onRetry={() => { void query.refetch(); }}
        retrying={query.isFetching && !query.isFetchingNextPage}
      />
      <MyListFilterBar
        idPrefix="my-orders"
        searchLabel="주문 번호·상품명 검색"
        searchPlaceholder="예: 123 또는 머그"
        filters={filters}
        statusOptions={statusOptions}
        quickTabs={quickTabs}
        sortOptions={ORDER_SORT_OPTIONS}
        count={orders.length}
        hasMore={query.hasNextPage}
      />
      {query.isLoading && <LoadingSpinner />}
      {hasLoadedOrders && orders.length === 0 && <EmptyState message="검색 조건에 맞는 주문 내역이 없습니다." />}
      {orders.length > 0 && orders.map((order) => (
        <Card
          key={order.orderId}
          as={Link}
          to={{ pathname: `/my/orders/${order.orderId}`, search }}
          className="mb-2 text-decoration-none my-list-card border-0"
        >
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">주문 #{order.orderId}</div>
                <OrderItemSummary items={order.items} />
                <small className="text-muted-soft">
                  {order.paidAt ? `결제 ${formatDateTime(order.paidAt)}` : formatDateTime(order.createdAt)}
                </small>
              </Col>
              <Col xs={6} md={3}>
                <StatusBadge status={order.status} />
              </Col>
              <Col xs={6} md={5} className="text-md-end">
                <small>{formatKRW(order.totalAmount)}</small>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
      <CursorLoadMore query={query} label="주문 더 보기" loadingLabel="주문 불러오는 중..." />
    </MySectionPage>
  );
}
