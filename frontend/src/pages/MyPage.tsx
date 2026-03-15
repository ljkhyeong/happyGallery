import { useEffect, useState } from "react";
import { Container, Card, Row, Col, Badge, Button } from "react-bootstrap";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { GuestClaimModal } from "@/features/customer-claim/GuestClaimModal";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { fetchMyBookings, fetchMyOrders, fetchMyPasses } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { LoadingSpinner, ErrorAlert, EmptyState, StatusBadge } from "@/shared/ui";
import { formatKRW, formatDateTime } from "@/shared/lib";

export function MyPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [showClaimModal, setShowClaimModal] = useState(false);
  const [showClaimEntryHint, setShowClaimEntryHint] = useState(false);
  const { user, isAuthenticated, isLoading: authLoading, logout, refresh } = useCustomerAuth();

  const { data: orders, isLoading: ordersLoading, error: ordersError } = useQuery({
    queryKey: ["my", "orders"],
    queryFn: fetchMyOrders,
    enabled: isAuthenticated,
  });

  const { data: bookings, isLoading: bookingsLoading, error: bookingsError } = useQuery({
    queryKey: ["my", "bookings"],
    queryFn: fetchMyBookings,
    enabled: isAuthenticated,
  });

  const { data: passes, isLoading: passesLoading, error: passesError } = useQuery({
    queryKey: ["my", "passes"],
    queryFn: fetchMyPasses,
    enabled: isAuthenticated,
  });

  const orderCount = orders?.length ?? 0;
  const bookingCount = bookings?.length ?? 0;
  const passCount = passes?.length ?? 0;
  const remainingCredits = passes?.reduce((sum, pass) => sum + pass.remainingCredits, 0) ?? 0;
  const nextBooking = bookings
    ?.filter((booking) => booking.status === "BOOKED")
    .sort((a, b) => new Date(a.startAt).getTime() - new Date(b.startAt).getTime())[0];
  const latestOrder = orders?.[0];
  const activePass = passes?.find((pass) => pass.remainingCredits > 0) ?? passes?.[0];

  useEffect(() => {
    if (!isAuthenticated || searchParams.get("claim") !== "1") {
      return;
    }
    setShowClaimEntryHint(true);
    setShowClaimModal(true);
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete("claim");
    setSearchParams(nextSearchParams, { replace: true });
  }, [isAuthenticated, searchParams, setSearchParams]);

  if (authLoading) return <Container className="page-container"><LoadingSpinner /></Container>;

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 760 }}>
        <Badge bg="light" text="dark" className="mb-3">Member Self Service</Badge>
        <MyAuthGateCard
          title="로그인하고 주문, 예약, 8회권을 한 곳에서 관리하세요"
          description="회원은 추가 휴대폰 인증 없이 내 주문과 예약, 8회권을 바로 확인할 수 있습니다. 비회원 조회가 필요하면 guest 경로를 그대로 사용할 수 있습니다."
          showGuestLinks
        />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <Card className="my-dashboard-hero mb-4 border-0">
        <Card.Body>
          <div className="d-flex flex-column flex-lg-row justify-content-between gap-4">
            <div className="flex-grow-1">
              <div className="my-section-kicker mb-2">Member Self Service</div>
              <h3 className="mb-2">{user!.name}님, 다시 오셨네요</h3>
              <p className="text-muted-soft mb-3">
                최근 주문, 예약, 8회권 현황을 이 페이지에서 바로 관리할 수 있습니다.
              </p>
              <div className="d-flex flex-wrap gap-2 align-items-center mb-3">
                <Badge bg={user!.phoneVerified ? "success" : "secondary"}>
                  {user!.phoneVerified ? "휴대폰 인증 완료" : "휴대폰 재확인 필요"}
                </Badge>
                <span className="text-muted-soft small">{user!.email}</span>
                <span className="text-muted-soft small">{user!.phone}</span>
              </div>
              {nextBooking && (
                <div className="my-dashboard-note">
                  다음 예약: <strong>{nextBooking.className}</strong> · {formatDateTime(nextBooking.startAt)}
                </div>
              )}
            </div>
            <div className="d-flex flex-wrap align-content-start gap-2">
              <Button as={Link as any} to="/products" variant="dark" size="sm">
                상품 보러가기
              </Button>
              <Button as={Link as any} to="/bookings/new" variant="outline-primary" size="sm">
                체험 예약
              </Button>
              <Button as={Link as any} to="/passes/purchase" variant="outline-primary" size="sm">
                8회권 구매
              </Button>
              <Button variant="outline-secondary" size="sm" onClick={() => { logout(); navigate("/"); }}>
                로그아웃
              </Button>
            </div>
          </div>
        </Card.Body>
      </Card>

      <Row className="g-3 mb-4">
        <Col md={4}>
          <Card className="my-stat-card h-100 border-0">
            <Card.Body>
              <div className="my-section-kicker mb-2">Orders</div>
              <div className="my-stat-value">{orderCount}</div>
              <div className="text-muted-soft small">
                {latestOrder ? `최근 주문 ${formatDateTime(latestOrder.createdAt)}` : "아직 주문이 없습니다."}
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="my-stat-card h-100 border-0">
            <Card.Body>
              <div className="my-section-kicker mb-2">Bookings</div>
              <div className="my-stat-value">{bookingCount}</div>
              <div className="text-muted-soft small">
                {nextBooking ? `다음 일정 ${formatDateTime(nextBooking.startAt)}` : "예정된 예약이 없습니다."}
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col md={4}>
          <Card className="my-stat-card h-100 border-0">
            <Card.Body>
              <div className="my-section-kicker mb-2">Passes</div>
              <div className="my-stat-value">{remainingCredits}</div>
              <div className="text-muted-soft small">
                {activePass ? `활성 8회권 ${passCount}건` : "보유한 8회권이 없습니다."}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      <Card className="mb-4 my-claim-card border-0">
        <Card.Body>
          {showClaimEntryHint && (
            <div className="my-claim-entry-note mb-3">
              <div>
                <strong>회원가입이 완료되었습니다.</strong>
                <div className="small text-muted-soft">
                  같은 번호의 비회원 이력이 있다면 지금 바로 가져올 수 있습니다.
                </div>
              </div>
              <div className="d-flex flex-wrap gap-2">
                <Button size="sm" variant="dark" onClick={() => setShowClaimModal(true)}>
                  지금 확인
                </Button>
                <Button size="sm" variant="outline-secondary" onClick={() => setShowClaimEntryHint(false)}>
                  닫기
                </Button>
              </div>
            </div>
          )}
          <div className="d-flex justify-content-between align-items-start gap-3">
            <div>
              <div className="my-section-kicker mb-2">Guest Claim</div>
              <h6 className="mb-1">비회원 이력 가져오기</h6>
              <p className="text-muted-soft small mb-0">
                {user!.phoneVerified
                  ? "같은 휴대폰 번호로 남긴 비회원 주문, 예약, 8회권을 이 계정으로 이전할 수 있습니다."
                  : "먼저 같은 번호인지 한 번 더 확인한 뒤 비회원 주문, 예약, 8회권을 가져올 수 있습니다."}
              </p>
            </div>
            <Button
              variant={user!.phoneVerified ? "outline-primary" : "primary"}
              size="sm"
              onClick={() => setShowClaimModal(true)}
            >
              {user!.phoneVerified ? "이력 가져오기" : "휴대폰 확인 후 가져오기"}
            </Button>
          </div>
        </Card.Body>
      </Card>

      <section id="my-orders" className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <div>
            <h6 className="mb-1">내 주문</h6>
            <p className="text-muted-soft small mb-0">최근 5건의 주문 진행 상태를 빠르게 확인합니다.</p>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span className="text-muted-soft small">총 {orderCount}건</span>
            <Link to="/my/orders" className="my-inline-link small">전체 보기</Link>
          </div>
        </div>
        {ordersLoading && <LoadingSpinner />}
        <ErrorAlert error={ordersError} />
        {orders && orders.length === 0 && <EmptyState message="주문 내역이 없습니다." />}
        {orders && orders.length > 0 && orders.slice(0, 5).map((o) => (
          <Card key={o.orderId} as={Link} to={`/my/orders/${o.orderId}`} className="mb-2 text-decoration-none my-list-card border-0">
            <Card.Body className="py-3 px-3">
              <Row className="align-items-center g-2">
                <Col xs={12} md={4}>
                  <div className="fw-semibold small">주문 #{o.orderId}</div>
                  <small className="text-muted-soft">
                    {o.paidAt ? `결제 ${formatDateTime(o.paidAt)}` : formatDateTime(o.createdAt)}
                  </small>
                </Col>
                <Col xs={6} md={3}>
                  <StatusBadge status={o.status} />
                </Col>
                <Col xs={6} md={5} className="text-md-end">
                  <small>{formatKRW(o.totalAmount)}</small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        ))}
        {orders && orders.length > 5 && (
          <p className="text-muted-soft small mt-2 mb-0">최근 5건만 표시합니다.</p>
        )}
      </section>

      <section id="my-bookings" className="mb-4">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <div>
            <h6 className="mb-1">내 예약</h6>
            <p className="text-muted-soft small mb-0">다가오는 클래스와 예약 상태를 확인하고 상세로 이동합니다.</p>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span className="text-muted-soft small">총 {bookingCount}건</span>
            <Link to="/my/bookings" className="my-inline-link small">전체 보기</Link>
          </div>
        </div>
        {bookingsLoading && <LoadingSpinner />}
        <ErrorAlert error={bookingsError} />
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

      <section id="my-passes">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <div>
            <h6 className="mb-1">내 8회권</h6>
            <p className="text-muted-soft small mb-0">남은 횟수와 만료일을 기준으로 현재 사용 가능한 8회권을 확인합니다.</p>
          </div>
          <div className="d-flex align-items-center gap-3">
            <span className="text-muted-soft small">총 {passCount}건</span>
            <Link to="/my/passes" className="my-inline-link small">전체 보기</Link>
          </div>
        </div>
        {passesLoading && <LoadingSpinner />}
        <ErrorAlert error={passesError} />
        {passes && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
        {passes && passes.length > 0 && passes.map((p) => (
          <Card key={p.passId} className="mb-2 my-list-card border-0">
            <Card.Body className="py-3 px-3">
              <Row className="align-items-center g-2">
                <Col xs={12} md={4}>
                  <div className="fw-semibold small">8회권 #{p.passId}</div>
                  <small className="text-muted-soft">구매 {formatDateTime(p.purchasedAt)}</small>
                </Col>
                <Col xs={6} md={4}>
                  <small>잔여 <strong>{p.remainingCredits}</strong>/{p.totalCredits}회</small>
                </Col>
                <Col xs={6} md={4} className="text-md-end">
                  <small className="text-muted-soft">~{formatDateTime(p.expiresAt)}</small>
                </Col>
              </Row>
            </Card.Body>
          </Card>
        ))}
      </section>

      {showClaimModal && (
        <GuestClaimModal
          show={showClaimModal}
          onClose={() => setShowClaimModal(false)}
          phone={user!.phone}
          phoneVerified={user!.phoneVerified}
          onPhoneVerified={refresh}
        />
      )}
    </Container>
  );
}
