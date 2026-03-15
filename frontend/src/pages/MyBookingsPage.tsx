import { useQuery } from "@tanstack/react-query";
import { Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useSearchParams } from "react-router-dom";
import { fetchMyBookings } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge, getStatusLabel } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";

export function MyBookingsPage() {
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const { data: bookings, isLoading, error } = useQuery({
    queryKey: ["my", "bookings"],
    queryFn: fetchMyBookings,
    enabled: isAuthenticated,
  });
  const searchQuery = searchParams.get("q") ?? "";
  const statusFilter = searchParams.get("status") ?? "ALL";
  const statusOptions = [
    { value: "ALL", label: "전체 상태" },
    ...Array.from(new Set((bookings ?? []).map((booking) => booking.status))).map((status) => ({
      value: status,
      label: getStatusLabel(status),
    })),
  ];
  const filteredBookings = (bookings ?? []).filter((booking) => {
    const matchesStatus = statusFilter === "ALL" || booking.status === statusFilter;
    const normalizedQuery = searchQuery.trim().toLowerCase();
    const matchesQuery =
      normalizedQuery === "" ||
      String(booking.bookingId).includes(normalizedQuery) ||
      booking.className.toLowerCase().includes(normalizedQuery);
    return matchesStatus && matchesQuery;
  });

  function updateFilters(next: { q?: string; status?: string }) {
    const nextSearchParams = new URLSearchParams(searchParams);
    const nextQuery = next.q ?? searchQuery;
    const nextStatus = next.status ?? statusFilter;

    if (nextQuery.trim()) nextSearchParams.set("q", nextQuery.trim());
    else nextSearchParams.delete("q");

    if (nextStatus !== "ALL") nextSearchParams.set("status", nextStatus);
    else nextSearchParams.delete("status");

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
          <Button as={Link as any} to="/bookings/new" variant="outline-secondary" size="sm">
            새 예약 만들기
          </Button>
        </div>
        <div className="my-section-kicker mb-2">My Bookings</div>
        <h4 className="mb-2">전체 예약</h4>
        <p className="text-muted-soft small mb-0">
          다가오는 클래스와 지난 예약 상태를 확인하고 상세 페이지에서 변경이나 취소를 이어갈 수 있습니다.
        </p>
      </div>

      <ErrorAlert error={error} />
      {bookings && bookings.length > 0 && (
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
          resultText={`${filteredBookings.length} / ${bookings.length}건 표시 중`}
          onReset={() => setSearchParams({}, { replace: true })}
        />
      )}
      {bookings && bookings.length === 0 && <EmptyState message="예약 내역이 없습니다." />}
      {bookings && bookings.length > 0 && filteredBookings.length === 0 && (
        <EmptyState message="필터 조건에 맞는 예약이 없습니다." />
      )}
      {filteredBookings.length > 0 && filteredBookings.map((booking) => (
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
                <small className="text-muted-soft">{formatDateTime(booking.startAt)}</small>
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
    </Container>
  );
}
