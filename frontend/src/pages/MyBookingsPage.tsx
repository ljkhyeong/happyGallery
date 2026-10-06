import { ListMyBookingsPageSort, ListMyBookingsPageStatus } from "@/generated/api/booking";
import { useDebouncedValue } from "@/shared/hooks/useDebouncedValue";
import { LinkButton } from "@/shared/ui/LinkButton";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Button, Card, Col, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { fetchMyBookingsPage } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { bookingDepositLabel, buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyListFilters } from "@/features/my/useMyListFilters";
import { myNavLabel } from "@/features/my/myNavigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys } from "@/shared/api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, PageHeader } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

const DEFAULT_SORT = "SOONEST";
const BOOKING_SORT_OPTIONS = [
  { value: "SOONEST", label: "예약일 빠른순" },
  { value: "LATEST", label: "예약일 늦은순" },
  { value: "DEPOSIT_DESC", label: "예약금 높은순" },
];

export function MyBookingsPage() {
  const { search } = useLocation();
  const { isAuthenticated } = useCustomerAuth();
  const { searchQuery, statusFilter, sortValue, updateFilters, resetFilters } =
    useMyListFilters({
      defaultSort: DEFAULT_SORT,
      statusValues: Object.values(ListMyBookingsPageStatus),
      sortValues: BOOKING_SORT_OPTIONS.map((option) => option.value),
    });
  const debouncedSearch = useDebouncedValue(searchQuery, 300);
  const filters = {
    keyword: debouncedSearch.trim() || undefined,
    status: statusFilter === "ALL" ? undefined : statusFilter as ListMyBookingsPageStatus,
    sort: sortValue as ListMyBookingsPageSort,
  };
  const {
    data: bookingsData,
    isLoading,
    isFetching,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: [...queryKeys.member.bookings.history, filters],
    queryFn: ({ pageParam, signal }) => fetchMyBookingsPage(pageParam, signal, filters),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
  });
  const bookings = bookingsData?.pages.flatMap((page) => page.content) ?? [];
  const hasLoadedBookings = bookingsData !== undefined;
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(Object.values(ListMyBookingsPageStatus)),
  ];
  const quickTabs = [
    { value: "ALL", label: "전체" },
    ...buildStatusFilterOptions(["BOOKED", "COMPLETED", "CANCELED"]),
  ];

  return (
    <>
      <PageHeader
        title={myNavLabel("/my/bookings")}
        description="예약 일정과 변경·취소 가능 여부를 확인하세요."
        actions={(
          <LinkButton to="/bookings/new" variant="outline-dark" size="sm">새 예약 만들기</LinkButton>
        )}
      />

      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
      />
      <MyListFilterBar
        idPrefix="my-bookings"
        searchLabel="예약 검색"
        searchPlaceholder="예약 번호 또는 클래스명"
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
        sortOptions={BOOKING_SORT_OPTIONS}
        onSortChange={(value) => updateFilters({ sort: value })}
        defaultSortValue={DEFAULT_SORT}
        resultText={`검색 결과 ${bookings.length}건 표시 중${hasNextPage ? " · 더 보기로 계속 조회" : ""}`}
        onReset={resetFilters}
      />
      {isLoading && <LoadingSpinner />}
      {hasLoadedBookings && bookings.length === 0 && <EmptyState message="검색 조건에 맞는 예약 내역이 없습니다." />}
      {bookings.length > 0 && bookings.map((booking) => (
        <Card
          key={booking.bookingId}
          as={Link}
          to={{ pathname: `/my/bookings/${booking.bookingId}`, search }}
          className="mb-2 text-decoration-none my-list-card border-0"
        >
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={5}>
                <div className="fw-semibold small">{booking.className}</div>
                <small className="text-muted-soft">
                  {formatDateTime(booking.startAt)} · {booking.participantCount}명
                </small>
              </Col>
              <Col xs={6} md={3}>
                <StatusBadge status={booking.status} />
              </Col>
              <Col xs={6} md={4} className="text-md-end">
                <small>{bookingDepositLabel(booking.depositAmount)}</small>
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
            {isFetchingNextPage ? "예약 불러오는 중..." : "예약 더 보기"}
          </Button>
        </div>
      )}
    </>
  );
}
