import { useQuery } from "@tanstack/react-query";
import { Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useSearchParams } from "react-router-dom";
import { fetchMyOrders } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildQuickStatusTabs, buildStatusFilterOptions } from "@/features/my/listUtils";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, getStatusLabel } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

const DEFAULT_SORT = "LATEST";
const ORDER_SORT_OPTIONS = [
  { value: "LATEST", label: "최신 주문순" },
  { value: "OLDEST", label: "오래된 주문순" },
  { value: "AMOUNT_DESC", label: "결제 금액 높은순" },
  { value: "AMOUNT_ASC", label: "결제 금액 낮은순" },
];

export function MyOrdersPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: orders, isLoading, error } = useQuery({
    queryKey: ["my", "orders"],
    queryFn: fetchMyOrders,
    enabled: isAuthenticated,
  });
  const searchQuery = searchParams.get("q") ?? "";
  const statusFilter = searchParams.get("status") ?? "ALL";
  const sortValue = searchParams.get("sort") ?? DEFAULT_SORT;
  const statuses = (orders ?? []).map((order) => order.status);
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(statuses),
  ];
  const quickTabs = buildQuickStatusTabs(statuses);
  const filteredOrders = (orders ?? []).filter((order) => {
    const matchesStatus = statusFilter === "ALL" || order.status === statusFilter;
    const normalizedQuery = searchQuery.trim();
    const matchesQuery = normalizedQuery === "" || String(order.orderId).includes(normalizedQuery);
    return matchesStatus && matchesQuery;
  });
  const sortedOrders = [...filteredOrders].sort((left, right) => {
    switch (sortValue) {
      case "OLDEST":
        return new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime();
      case "AMOUNT_DESC":
        return right.totalAmount - left.totalAmount;
      case "AMOUNT_ASC":
        return left.totalAmount - right.totalAmount;
      case "LATEST":
      default:
        return new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    }
  });
  const activeCount = (orders ?? []).filter((order) =>
    !["PICKED_UP", "DELIVERED", "REJECTED", "AUTO_REFUND_TIMEOUT", "PICKUP_EXPIRED"].includes(order.status),
  ).length;
  const completedCount = (orders ?? []).filter((order) =>
    ["PICKED_UP", "DELIVERED"].includes(order.status),
  ).length;

  function updateFilters(next: { q?: string; status?: string; sort?: string }) {
    const nextSearchParams = new URLSearchParams(searchParams);
    const nextQuery = next.q ?? searchQuery;
    const nextStatus = next.status ?? statusFilter;
    const nextSort = next.sort ?? sortValue;

    if (nextQuery.trim()) nextSearchParams.set("q", nextQuery.trim());
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");

    if (nextSort !== DEFAULT_SORT) nextSearchParams.set("sort", nextSort);
    else nextSearchParams.delete("sort");

    setSearchParams(nextSearchParams, { replace: true });
  }

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 720 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 주문 목록은 로그인 후 내 정보에서 바로 확인할 수 있습니다."
        />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my" className="text-decoration-none small">
            &larr; 내 정보
          </Link>
          <Button as={Link as any} to="/products" variant="outline-secondary" size="sm">
            상품 보러가기
          </Button>
        </div>
        <div className="my-section-kicker mb-2">My Orders</div>
        <h4 className="mb-2">전체 주문</h4>
        <p className="text-muted-soft small mb-0">
          최근 주문부터 상태와 결제 금액을 확인하고, 빠른 상태 탭과 정렬로 원하는 주문을 바로 찾습니다.
        </p>
      </div>

      <ErrorAlert error={error} />
      {orders && orders.length > 0 && (
        <div className="my-list-summary mb-3">
          <span className="my-summary-chip">진행 중 {activeCount}건</span>
          <span className="my-summary-chip">완료 {completedCount}건</span>
          <span className="my-summary-chip">
            현재 필터 {statusFilter === "ALL" ? "전체 상태" : getStatusLabel(statusFilter)}
          </span>
        </div>
      )}
      {orders && orders.length > 0 && (
        <MyListFilterBar
          idPrefix="my-orders"
          searchLabel="주문 번호 검색"
          searchPlaceholder="예: 123"
          searchValue={searchQuery}
          onSearchChange={(value) => updateFilters({ q: value })}
          filterLabel="상태"
          filterValue={statusFilter}
          filterOptions={statusOptions}
          onFilterChange={(value) => updateFilters({ status: value })}
          quickTabs={quickTabs}
          activeTabValue={statusFilter}
          onTabChange={(value) => updateFilters({ status: value })}
          sortLabel="정렬"
          sortValue={sortValue}
          sortOptions={ORDER_SORT_OPTIONS}
          onSortChange={(value) => updateFilters({ sort: value })}
          defaultSortValue={DEFAULT_SORT}
          resultText={`${sortedOrders.length} / ${orders.length}건 표시 중`}
          onReset={() => setSearchParams({}, { replace: true })}
        />
      )}
      {orders && orders.length === 0 && <EmptyState message="주문 내역이 없습니다." />}
      {orders && orders.length > 0 && sortedOrders.length === 0 && (
        <EmptyState message="필터 조건에 맞는 주문이 없습니다." />
      )}
      {sortedOrders.length > 0 && sortedOrders.map((order) => (
        <Card
          key={order.orderId}
          as={Link}
          to={`/my/orders/${order.orderId}`}
          className="mb-2 text-decoration-none my-list-card border-0"
        >
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                  <div className="fw-semibold small">주문 #{order.orderId}</div>
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
    </Container>
  );
}
