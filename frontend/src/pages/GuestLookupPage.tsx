import { useEffect } from "react";
import { Badge, Button, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { trackClientEvent, trackGuestMemberCta } from "@/features/monitoring/api";

export function GuestLookupPage() {
  const location = useLocation();
  const monitoringSource = (location.state as { monitoringSource?: string } | null)?.monitoringSource ?? "direct";
  const claimLoginHref = buildAuthPageHref("/login", {
    redirectTo: "/my?claim=1",
    claim: true,
  });
  const claimSignupHref = buildAuthPageHref("/signup", {
    redirectTo: "/my?claim=1",
    claim: true,
  });

  useEffect(() => {
    trackClientEvent({
      event: "GUEST_LOOKUP_HUB_VIEWED",
      path: "/guest",
      source: monitoringSource,
      target: "hub",
    });
  }, [monitoringSource]);

  return (
    <Container className="page-container" style={{ maxWidth: 920 }}>
      <Card className="lookup-panel border-0 mb-4">
        <Card.Body className="p-4 p-lg-5">
          <Badge bg="light" text="dark" className="mb-3">Guest Support Route</Badge>
          <h3 className="mb-2">비회원 조회 안내</h3>
          <p className="text-muted-soft mb-3">
            비회원 조회는 이미 완료한 주문과 예약을 확인하는 보조 경로입니다.
            반복 조회나 이후 관리가 필요하면 회원 전환 후 <strong>`/my`</strong>에서 이어서 보는 흐름을 권장합니다.
          </p>
          <div className="guest-route-note mb-0">
            <div className="guest-route-note-title">권장 사용 순서</div>
            <div className="small text-muted-soft">
              1. 지금은 비회원 토큰으로 조회
              <br />
              2. 계속 관리할 예정이면 로그인 또는 회원가입
              <br />
              3. 같은 번호의 이력을 `/my`에서 claim
            </div>
          </div>
        </Card.Body>
      </Card>

      <Row xs={1} md={2} className="g-3 mb-4">
        <Col>
          <Link to="/guest/orders" className="store-feature-card h-100">
            <div className="store-feature-kicker">Guest Order Lookup</div>
            <div className="store-feature-title">비회원 주문 조회</div>
            <p className="store-feature-desc">
              주문 완료 후 받은 주문 ID와 access token으로 현재 주문 상태를 확인합니다.
            </p>
            <span className="store-feature-cta">주문 조회로 이동 &rarr;</span>
          </Link>
        </Col>
        <Col>
          <Link to="/guest/bookings" className="store-feature-card h-100 store-feature-card-accent">
            <div className="store-feature-kicker">Guest Booking Lookup</div>
            <div className="store-feature-title">비회원 예약 조회</div>
            <p className="store-feature-desc">
              예약 ID와 access token으로 조회하고, 같은 경로에서 변경과 취소까지 이어서 처리합니다.
            </p>
            <span className="store-feature-cta">예약 조회로 이동 &rarr;</span>
          </Link>
        </Col>
      </Row>

      <Card className="my-claim-card border-0">
        <Card.Body className="d-flex flex-column flex-lg-row justify-content-between gap-3 align-items-start">
          <div>
            <div className="my-section-kicker mb-2">Move To Member Flow</div>
            <h5 className="mb-2">비회원 이력을 회원 내 정보로 가져오세요</h5>
            <p className="text-muted-soft small mb-0">
              로그인 또는 회원가입 후 같은 번호의 비회원 주문과 예약을 `/my`에서 가져오면
              이후에는 추가 토큰 입력 없이 한 화면에서 관리할 수 있습니다.
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <Button
              as={Link as any}
              to={claimLoginHref}
              variant="dark"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_lookup_hub", "login")}
            >
              로그인하고 가져오기
            </Button>
            <Button
              as={Link as any}
              to={claimSignupHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_lookup_hub", "signup")}
            >
              회원가입
            </Button>
          </div>
        </Card.Body>
      </Card>
    </Container>
  );
}
