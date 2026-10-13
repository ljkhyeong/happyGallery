import { useEffect, useState } from "react";
import { Accordion, Container } from "react-bootstrap";
import { useLocation, useNavigate } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { trackClientEvent } from "@/features/monitoring/api";
import { GuestRecordRecoverySection } from "@/features/guest-recovery/GuestRecordRecoverySection";
import { GuestPaymentStatusRecoverySection } from "@/features/guest-payment-recovery/GuestPaymentStatusRecoverySection";
import { GuestLookupForm, type GuestLookupKind } from "@/features/guest-lookup/GuestLookupForm";
import { GuestLookupPanel } from "@/features/guest-lookup/GuestLookupPanel";
import { GuestMemberGuide } from "@/features/guest-lookup/GuestMemberGuide";
import { captureCustomerSession } from "@/shared/api";

export function GuestLookupPage() {
  const { sessionVersion } = useCustomerAuth();
  return <GuestLookupContent key={sessionVersion} />;
}

/** 다른 화면에서 도움말 칸을 바로 열어 보낼 때 쓰는 주소 해시. */
const HELP_BY_HASH: Record<string, string> = {
  "#lookup-help": "records",
  "#payment-help": "payments",
};

function GuestLookupContent() {
  const location = useLocation();
  const navigate = useNavigate();
  const [activeHelp, setActiveHelp] = useState<string | null>(HELP_BY_HASH[location.hash] ?? null);
  // 모바일 하단 "예약 조회"처럼 예약으로 들어오면 예약 조회 칸을 먼저 연다.
  const [kind, setKind] = useState<GuestLookupKind>(
    new URLSearchParams(location.search).get("kind") === "bookings" ? "bookings" : "orders",
  );

  useEffect(() => {
    trackClientEvent("GUEST_LOOKUP_HUB_VIEWED");
  }, []);

  return (
    <Container className="page-container guest-lookup-page">
      <GuestLookupPanel title="비회원 조회" kind={kind} onKindChange={setKind}
        onHelpRequest={() => setActiveHelp("records")}>
        <GuestLookupForm key={kind} kind={kind} onLookup={(id, token) => {
          const idKey = kind === "orders" ? "orderId" : "bookingId";
          navigate(`/guest/${kind}?${idKey}=${id}`, {
            state: { [idKey]: id, token, customerSession: captureCustomerSession() },
          });
        }} />
      </GuestLookupPanel>

      <section id="lookup-help" className="guest-lookup-help" aria-labelledby="guest-help-title">
        <h2 id="guest-help-title">조회에 도움이 필요하신가요?</h2>
        <Accordion activeKey={activeHelp} onSelect={(key) =>
          setActiveHelp(Array.isArray(key) ? key[0] ?? null : key ?? null)}>
          <Accordion.Item eventKey="records">
            <Accordion.Header>주문·예약 번호나 조회 코드를 잊었어요</Accordion.Header>
            <Accordion.Body><GuestRecordRecoverySection /></Accordion.Body>
          </Accordion.Item>
          <Accordion.Item eventKey="payments" id="payment-help">
            <Accordion.Header>결제는 했는데 완료 여부를 모르겠어요</Accordion.Header>
            <Accordion.Body><GuestPaymentStatusRecoverySection /></Accordion.Body>
          </Accordion.Item>
        </Accordion>
      </section>
      <GuestMemberGuide />
    </Container>
  );
}
