import { LinkButton } from "@/shared/ui/LinkButton";
import { useInfiniteQuery } from "@tanstack/react-query";
import { Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link } from "react-router";
import { fetchMyBookingsPage } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { buildQuickStatusTabs, buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyListFilters } from "@/features/my/useMyListFilters";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys } from "@/shared/api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, getStatusLabel } from "@/shared/ui";
import { formatDateTime, formatKRW, parseApiDateTime } from "@/shared/lib";

const DEFAULT_SORT = "SOONEST";
const BOOKING_SORT_OPTIONS = [
  { value: "SOONEST", label: "예약일 빠른순" },
  { value: "LATEST", label: "예약일 늦은순" },
  { value: "DEPOSIT_DESC", label: "예약금 높은순" },
];

export function MyBookingsPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const { searchQuery, statusFilter, sortValue, updateFilters, resetFilters } =
    useMyListFilters({ defaultSort: DEFAULT_SORT });
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
    queryKey: queryKeys.member.bookings.history,
    queryFn: ({ pageParam, signal }) => fetchMyBookingsPage(pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
  });
  const bookings = bookingsData?.pages.flatMap((page) => page.content) ?? [];
  const hasLoadedBookings = bookingsData !== undefined;
  const normalizedQuery = searchQuery.trim().toLowerCase();
  const statuses = bookings.map((booking) => booking.status);
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(statuses),
  ];
  const quickTabs = buildQuickStatusTabs(statuses);
  const filteredBookings = bookings.filter((booking) => {
    const matchesStatus = statusFilter === "ALL" || booking.status === statusFilter;
    const matchesQuery =
      normalizedQuery === "" ||
      String(booking.bookingId).includes(normalizedQuery) ||
      booking.className.toLowerCase().includes(normalizedQuery);
    return matchesStatus && matchesQuery;
  });
  const sortedBookings = [...filteredBookings].sort((left, right) => {
    switch (sortValue) {
      case "LATEST":
        return parseApiDateTime(right.startAt) - parseApiDateTime(left.startAt);
      case "DEPOSIT_DESC":
        return right.depositAmount - left.depositAmount;
      case "SOONEST":
      default:
        return parseApiDateTime(left.startAt) - parseApiDateTime(right.startAt);
    }
  });
  const upcomingCount = bookings.filter((booking) =>
    booking.status === "BOOKED" && parseApiDateTime(booking.startAt) >= Date.now(),
  ).length;
  const finishedCount = bookings.filter((booking) =>
    ["COMPLETED", "CANCELED", "NO_SHOW"].includes(booking.status),
  ).length;

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 720 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 예약 목록은 로그인 후 내 정보에서 바로 확인할 수 있습니다."
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
          <LinkButton to="/bookings/new" variant="outline-secondary" size="sm">
            새 예약 만들기
          </LinkButton>
        </div>
        <div className="my-section-kicker mb-2">My Bookings</div>
        <h4 className="mb-2">전체 예약</h4>
        <p className="text-muted-soft small mb-0">
          다가오는 클래스와 지난 예약 상태를 상태 탭과 정렬로 나눠 보고, 상세 페이지에서 변경이나 취소를 이어갈 수 있습니다.
        </p>
      </div>

      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
      />
      {bookings.length > 0 && (
        <div className="my-list-summary mb-3">
          <span className="my-summary-chip">불러온 예약 중 다가오는 예약 {upcomingCount}건</span>
          <span className="my-summary-chip">불러온 예약 중 종료/취소 {finishedCount}건</span>
          <span className="my-summary-chip">
            현재 필터 {statusFilter === "ALL" ? "전체 상태" : getStatusLabel(statusFilter)}
          </span>
        </div>
      )}
      {bookings.length > 0 && (
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
          resultText={`${sortedBookings.length}건 표시 중 · 불러온 예약 ${bookings.length}건`}
          onReset={resetFilters}
        />
      )}
      {hasLoadedBookings && bookings.length === 0 && <EmptyState message="예약 내역이 없습니다." />}
      {bookings.length > 0 && sortedBookings.length === 0 && (
        <EmptyState message="필터 조건에 맞는 예약이 없습니다." />
      )}
      {sortedBookings.length > 0 && sortedBookings.map((booking) => (
        <Card
          key={booking.bookingId}
          as={Link}
          to={`/my/bookings/${booking.bookingId}`}
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
                <small>{formatKRW(booking.depositAmount)}</small>
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
    </Container>
  );
}
