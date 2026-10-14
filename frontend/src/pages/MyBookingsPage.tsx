import { ListMyBookingsPageStatus } from "@/generated/api/booking";
import { LinkButton } from "@/shared/ui/LinkButton";
import { Card, Col, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { fetchMyBookingsPage } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { bookingDepositLabel, buildStatusFilterOptions } from "@/features/my/listUtils";
import { useMyHistoryList } from "@/features/my/useMyHistoryList";
import { MySectionPage } from "@/features/my/MySectionPage";
import { queryKeys } from "@/shared/api";
import { CursorLoadMore, LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";

const DEFAULT_SORT = "SOONEST";
const BOOKING_SORT_OPTIONS = [
  { value: "SOONEST", label: "예약일 빠른순" },
  { value: "LATEST", label: "예약일 늦은순" },
  { value: "DEPOSIT_DESC", label: "예약금 높은순" },
] as const;

export function MyBookingsPage() {
  const { search } = useLocation();
  const { filters, query, items: bookings } = useMyHistoryList({
    queryKey: queryKeys.member.bookings.history,
    fetchPage: fetchMyBookingsPage,
    statusValues: Object.values(ListMyBookingsPageStatus),
    sortOptions: BOOKING_SORT_OPTIONS,
    defaultSort: DEFAULT_SORT,
  });
  const hasLoadedBookings = query.data !== undefined;
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...buildStatusFilterOptions(Object.values(ListMyBookingsPageStatus)),
  ];
  const quickTabs = [
    { value: "ALL", label: "전체" },
    ...buildStatusFilterOptions(["BOOKED", "COMPLETED", "CANCELED"]),
  ];

  return (
    <MySectionPage
      path="/my/bookings"
      actions={(
        <LinkButton to="/bookings/new" variant="outline-dark" size="sm">새 예약 만들기</LinkButton>
      )}
    >
      <ErrorAlert
        error={query.isFetchNextPageError ? null : query.error}
        onRetry={() => { void query.refetch(); }}
        retrying={query.isFetching && !query.isFetchingNextPage}
      />
      <MyListFilterBar
        idPrefix="my-bookings"
        searchLabel="예약 검색"
        searchPlaceholder="예약 번호 또는 클래스명"
        filters={filters}
        statusOptions={statusOptions}
        quickTabs={quickTabs}
        sortOptions={BOOKING_SORT_OPTIONS}
        count={bookings.length}
        hasMore={query.hasNextPage}
      />
      {query.isLoading && <LoadingSpinner />}
      {hasLoadedBookings && bookings.length === 0 && <EmptyState message="검색 조건에 맞는 예약 내역이 없습니다." />}
      {bookings.length > 0 && bookings.map((booking) => (
        <Card
          key={booking.bookingId}
          as={Link}
          to={{ pathname: `/my/bookings/${booking.bookingId}`, search }}
          className="mb-2 text-decoration-none my-list-card"
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
      <CursorLoadMore query={query} label="예약 더 보기" loadingLabel="예약 불러오는 중..." />
    </MySectionPage>
  );
}
