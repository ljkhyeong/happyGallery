import { OrderItemSummary } from "@/features/my/OrderItemSummary";
import { ListMyOrdersPageSort, ListMyOrdersPageStatus } from "@/generated/api/customerStore";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { LinkButton } from "@/shared/ui/LinkButton";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Button, Card, Col, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { fetchMyOrdersPage } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyListFilters } from "@/features/my/useMyListFilters";
import { myNavLabel } from "@/features/my/myNavigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys } from "@/shared/api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, PageHeader } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

const DEFAULT_SORT = "LATEST";
const ORDER_SORT_OPTIONS = [
  { value: "LATEST", label: "최신 주문순" },
  { value: "OLDEST", label: "오래된 주문순" },
  { value: "AMOUNT_DESC", label: "결제 금액 높은순" },
  { value: "AMOUNT_ASC", label: "결제 금액 낮은순" },
];

export function MyOrdersPage() {
  const { search } = useLocation();
  const { isAuthenticated } = useCustomerAuth();
  const { searchQuery, statusFilter, sortValue, updateFilters, resetFilters } =
    useMyListFilters({
      defaultSort: DEFAULT_SORT,
      statusValues: Object.values(ListMyOrdersPageStatus),
      sortValues: ORDER_SORT_OPTIONS.map((option) => option.value),
    });
  const debouncedSearch = useDebouncedValue(searchQuery, 300);
  const filters = {
    keyword: debouncedSearch.trim() || undefined,
    status: statusFilter === "ALL" ? undefined : statusFilter as ListMyOrdersPageStatus,
    sort: sortValue as ListMyOrdersPageSort,
  };
  const {
    data: ordersData,
    isLoading,
    isFetching,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: [...queryKeys.member.orders.history, filters],
    queryFn: ({ pageParam, signal }) => fetchMyOrdersPage(pageParam, signal, filters),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
  });
  const orders = ordersData?.pages.flatMap((page) => page.content) ?? [];
  const hasLoadedOrders = ordersData !== undefined;
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(Object.values(ListMyOrdersPageStatus)),
  ];
  const quickTabs = [
    { value: "ALL", label: "전체" },
    ...buildStatusFilterOptions(["PAID_APPROVAL_PENDING", "SHIPPED", "DELIVERED"]),
  ];

  return (
    <>
      <PageHeader
        title={myNavLabel("/my/orders")}
        description="주문 상태와 결제 금액을 확인하고 주문을 검색하세요."
        actions={(
          <LinkButton to="/products" variant="outline-dark" size="sm">작품 보러가기</LinkButton>
        )}
      />

      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
      />
      <MyListFilterBar
        idPrefix="my-orders"
        searchLabel="주문 번호·상품명 검색"
        searchPlaceholder="예: 123 또는 머그"
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
        resultText={`검색 결과 ${orders.length}건 표시 중${hasNextPage ? " · 더 보기로 계속 조회" : ""}`}
        onReset={resetFilters}
      />
      {isLoading && <LoadingSpinner />}
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
      {hasNextPage && (
        <div className="d-grid mt-3">
          <Button
            type="button"
            variant="outline-primary"
            disabled={isFetchingNextPage}
            onClick={() => { void fetchNextPage(); }}
          >
            {isFetchingNextPage ? "주문 불러오는 중..." : "주문 더 보기"}
          </Button>
        </div>
      )}
    </>
  );
}
