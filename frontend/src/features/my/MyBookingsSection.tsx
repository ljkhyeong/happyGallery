import { Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { MyBookingSummary } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime } from "@/shared/lib";

interface Props {
  bookings: MyBookingSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  totalCount: number;
}

export function MyBookingsSection({ bookings, isLoading, error, totalCount }: Props) {
  return (
    <section id="my-bookings" className="mb-4">
      <div className="d-flex justify-content-between align-items-center mb-2">
        <div>
          <h6 className="mb-1">내 예약</h6>
          <p className="text-muted-soft small mb-0">다가오는 클래스와 예약 상태를 확인하고 상세로 이동합니다.</p>
        </div>
        <div className="d-flex align-items-center gap-3">
          <span className="text-muted-soft small">총 {totalCount}건</span>
          <Link to="/my/bookings" className="my-inline-link small">전체 보기</Link>
        </div>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {bookings && bookings.length === 0 && <EmptyState message="예약 내역이 없습니다." />}
      {bookings && bookings.length > 0 && bookings.slice(0, 5).map((b) => (
        <Card
          key={b.bookingId}
          as={Link}
          to={`/my/bookings/${b.bookingId}`}
          className="mb-2 text-decoration-none my-list-card border-0"
        >
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={5}>
                <div className="fw-semibold small">{b.className}</div>
                <small className="text-muted-soft">{formatDateTime(b.startAt)}</small>
              </Col>
              <Col xs={6} md={3}>
                <StatusBadge status={b.status} />
              </Col>
              <Col xs={6} md={4} className="text-md-end">
                <small>{formatKRW(b.depositAmount)}</small>
              </Col>
            </Row>
          </Card.Body>
        </Card>
      ))}
      {bookings && bookings.length > 5 && (
        <p className="text-muted-soft small mt-2 mb-0">최근 5건만 표시합니다.</p>
      )}
    </section>
  );
}
