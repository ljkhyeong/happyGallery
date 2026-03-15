import { Container, Card, Row, Col, Badge, Button } from "react-bootstrap";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { api } from "@/shared/api";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatKRW, formatDateTime } from "@/shared/lib";

interface MyOrderSummary {
  orderId: number;
  status: string;
  totalAmount: number;
  paidAt: string;
  createdAt: string;
}

interface MyBookingSummary {
  bookingId: number;
  status: string;
  className: string;
  startAt: string;
  endAt: string;
  depositAmount: number;
}

interface MyPassSummary {
  passId: number;
  purchasedAt: string;
  expiresAt: string;
  totalCredits: number;
  remainingCredits: number;
  totalPrice: number;
}

export function MyPage() {
  const navigate = useNavigate();
  const { user, isAuthenticated, isLoading: authLoading, logout } = useCustomerAuth();

  const { data: orders, isLoading: ordersLoading, error: ordersError } = useQuery({
    queryKey: ["my", "orders"],
    queryFn: () => api<MyOrderSummary[]>("/me/orders"),
    enabled: isAuthenticated,
  });

  const { data: bookings, isLoading: bookingsLoading, error: bookingsError } = useQuery({
    queryKey: ["my", "bookings"],
    queryFn: () => api<MyBookingSummary[]>("/me/bookings"),
    enabled: isAuthenticated,
  });

  const { data: passes, isLoading: passesLoading, error: passesError } = useQuery({
    queryKey: ["my", "passes"],
    queryFn: () => api<MyPassSummary[]>("/me/passes"),
    enabled: isAuthenticated,
  });

  if (authLoading) return <Container className="page-container"><LoadingSpinner /></Container>;

  if (!isAuthenticated) {
    return (
      <Container className="page-container text-center" style={{ maxWidth: 480 }}>
        <h5 className="mb-3">로그인이 필요합니다</h5>
        <Button as={Link as any} to="/login" variant="primary">로그인</Button>
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      {/* 프로필 */}
      <Card className="mb-4">
        <Card.Body>
          <div className="d-flex justify-content-between align-items-center">
            <div>
              <h5 className="mb-1">{user!.name}</h5>
              <p className="text-muted-soft small mb-0">{user!.email}</p>
            </div>
            <Button variant="outline-secondary" size="sm" onClick={() => { logout(); navigate("/"); }}>
              로그아웃
            </Button>
          </div>
        </Card.Body>
      </Card>

      {/* 내 주문 */}
      <section className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h6 className="mb-0">내 주문</h6>
        </div>
        {ordersLoading && <LoadingSpinner />}
        <ErrorAlert error={ordersError} />
        {orders && orders.length === 0 && <EmptyState message="주문 내역이 없습니다." />}
        {orders && orders.length > 0 && orders.slice(0, 5).map((o) => (
          <Card key={o.orderId} as={Link} to={`/my/orders/${o.orderId}`} className="mb-2 text-decoration-none">
            <Card.Body className="py-2 px-3">
              <Row className="align-items-center">
                <Col xs={3}>
                  <small className="text-muted-soft">#{o.orderId}</small>
                </Col>
                <Col xs={3}>
                  <Badge bg="info" className="badge-status">{o.status}</Badge>
                </Col>
                <Col xs={3} className="text-end">
                  <small>{formatKRW(o.totalAmount)}</small>
                </Col>
                <Col xs={3} className="text-end">
                  <small className="text-muted-soft">{formatDateTime(o.createdAt)}</small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        ))}
      </section>

      {/* 내 예약 */}
      <section className="mb-4">
        <h6 className="mb-2">내 예약</h6>
        {bookingsLoading && <LoadingSpinner />}
        <ErrorAlert error={bookingsError} />
        {bookings && bookings.length === 0 && <EmptyState message="예약 내역이 없습니다." />}
        {bookings && bookings.length > 0 && bookings.slice(0, 5).map((b) => (
          <Card key={b.bookingId} className="mb-2">
            <Card.Body className="py-2 px-3">
              <Row className="align-items-center">
                <Col xs={4}>
                  <small className="fw-semibold">{b.className}</small>
                </Col>
                <Col xs={3}>
                  <Badge bg="info" className="badge-status">{b.status}</Badge>
                </Col>
                <Col xs={5} className="text-end">
                  <small className="text-muted-soft">{formatDateTime(b.startAt)}</small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        ))}
      </section>

      {/* 내 8회권 */}
      <section>
        <h6 className="mb-2">내 8회권</h6>
        {passesLoading && <LoadingSpinner />}
        <ErrorAlert error={passesError} />
        {passes && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
        {passes && passes.length > 0 && passes.map((p) => (
          <Card key={p.passId} className="mb-2">
            <Card.Body className="py-2 px-3">
              <Row className="align-items-center">
                <Col xs={4}>
                  <small>잔여 <strong>{p.remainingCredits}</strong>/{p.totalCredits}회</small>
                </Col>
                <Col xs={4} className="text-center">
                  <small className="text-muted-soft">{formatKRW(p.totalPrice)}</small>
                </Col>
                <Col xs={4} className="text-end">
                  <small className="text-muted-soft">~{formatDateTime(p.expiresAt)}</small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        ))}
      </section>
    </Container>
  );
}
