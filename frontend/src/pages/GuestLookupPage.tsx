import { LinkButton } from "@/shared/ui/LinkButton";
import { useEffect } from "react";
import { Badge, Card, Col, Container, Row } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { trackClientEvent, trackGuestMemberCta } from "@/features/monitoring/api";
import { GuestRecordRecoverySection } from "@/features/guest-recovery/GuestRecordRecoverySection";
import { GuestPaymentStatusRecoverySection } from "@/features/guest-payment-recovery/GuestPaymentStatusRecoverySection";

export function GuestLookupPage() {
  const { sessionVersion } = useCustomerAuth();
  return <GuestLookupContent key={sessionVersion} />;
}

function GuestLookupContent() {
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
          <Badge bg="light" text="dark" className="mb-3">비회원 조회</Badge>
          <h3 className="mb-2">비회원 조회 안내</h3>
          <p className="text-muted-soft mb-3">
            완료한 주문과 예약, 결제 도중 놓친 처리 결과를 여기서 확인할 수 있습니다.
            계속 관리할 예정이라면 회원가입 후 <strong>내 정보</strong>로 이력을 가져올 수 있습니다.
          </p>
          <div className="guest-route-note mb-0">
            <div className="guest-route-note-title">이용 방법</div>
            <div className="small text-muted-soft">
              1. 지금은 비회원 조회 정보로 확인
              <br />
              2. 계속 관리할 예정이면 로그인 또는 회원가입
              <br />
              3. 같은 번호의 이력을 회원 계정으로 가져오기
            </div>
          </div>
        </Card.Body>
      </Card>

      <GuestRecordRecoverySection />

      <GuestPaymentStatusRecoverySection />

      <Row xs={1} md={2} className="g-3 mb-4">
        <Col>
          <Link to="/guest/orders" className="store-feature-card h-100">
            <div className="store-feature-kicker">비회원 주문</div>
            <div className="store-feature-title">비회원 주문 조회</div>
            <p className="store-feature-desc">
              주문 완료 후 받은 주문 번호와 조회 코드로 현재 주문 상태를 확인합니다.
            </p>
            <span className="store-feature-cta">주문 조회로 이동 &rarr;</span>
          </Link>
        </Col>
        <Col>
          <Link to="/guest/bookings" className="store-feature-card h-100 store-feature-card-accent">
            <div className="store-feature-kicker">비회원 예약</div>
            <div className="store-feature-title">비회원 예약 조회</div>
            <p className="store-feature-desc">
              예약 번호와 조회 코드로 확인하고, 같은 화면에서 변경과 취소까지 이어서 처리합니다.
            </p>
            <span className="store-feature-cta">예약 조회로 이동 &rarr;</span>
          </Link>
        </Col>
      </Row>

      <Card className="my-claim-card border-0">
        <Card.Body className="d-flex flex-column flex-lg-row justify-content-between gap-3 align-items-start">
          <div>
            <div className="my-section-kicker mb-2">회원으로 계속 이용하기</div>
            <h5 className="mb-2">비회원 이력을 회원 내 정보로 가져오세요</h5>
            <p className="text-muted-soft small mb-0">
              로그인 또는 회원가입 후 같은 번호의 비회원 주문과 예약을 내 정보로 가져오면
              이후에는 조회 코드를 다시 입력하지 않고 한 화면에서 관리할 수 있습니다.
            </p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <LinkButton
              to={claimLoginHref}
              variant="dark"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_lookup_hub", "login")}
            >
              로그인하고 가져오기
            </LinkButton>
            <LinkButton
              to={claimSignupHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_lookup_hub", "signup")}
            >
              회원가입
            </LinkButton>
          </div>
        </Card.Body>
      </Card>
    </Container>
  );
}
