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
            비회원 주문·예약 내역과 결제 결과를 조회하세요.
          </p>
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
              주문 번호와 조회 코드로 주문 상태를 확인하세요.
            </p>
            <span className="store-feature-cta">주문 조회로 이동 &rarr;</span>
          </Link>
        </Col>
        <Col>
          <Link to="/guest/bookings" className="store-feature-card h-100 store-feature-card-accent">
            <div className="store-feature-kicker">비회원 예약</div>
            <div className="store-feature-title">비회원 예약 조회</div>
            <p className="store-feature-desc">
              예약 번호와 조회 코드로 예약을 조회·변경·취소하세요.
            </p>
            <span className="store-feature-cta">예약 조회로 이동 &rarr;</span>
          </Link>
        </Col>
      </Row>

      <Card className="my-claim-card border-0">
        <Card.Body className="d-flex flex-column flex-lg-row justify-content-between gap-3 align-items-start">
          <div>
            <div className="my-section-kicker mb-2">회원으로 계속 이용하기</div>
            <h5 className="mb-2">비회원 주문·예약을 내 정보로 가져오세요</h5>
            <p className="text-muted-soft small mb-0">
              로그인 또는 가입 후 같은 휴대폰 번호의 주문·예약을 가져오세요.
              가져온 내역은 조회 코드 없이 확인할 수 있습니다.
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
