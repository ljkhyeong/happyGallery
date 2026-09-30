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

function GuestLookupContent() {
  const location = useLocation();
  const navigate = useNavigate();
  const [activeHelp, setActiveHelp] = useState<string | null>(
    location.hash === "#lookup-help" ? "records" : null,
  );
  const [kind, setKind] = useState<GuestLookupKind>("orders");
  const monitoringSource = (location.state as { monitoringSource?: string } | null)?.monitoringSource ?? "direct";

  useEffect(() => {
    trackClientEvent({ event: "GUEST_LOOKUP_HUB_VIEWED", path: "/guest", source: monitoringSource, target: "hub" });
  }, [monitoringSource]);

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
          <Accordion.Item eventKey="payments">
            <Accordion.Header>결제는 했는데 완료 여부를 모르겠어요</Accordion.Header>
            <Accordion.Body><GuestPaymentStatusRecoverySection /></Accordion.Body>
          </Accordion.Item>
        </Accordion>
      </section>
      <GuestMemberGuide source="guest_lookup_hub" />
    </Container>
  );
}
