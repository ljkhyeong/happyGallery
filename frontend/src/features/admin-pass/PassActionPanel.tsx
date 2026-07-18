import { useState } from "react";
import { Card, Form, Button, Row, Col } from "react-bootstrap";
import { expirePasses, refundPass } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { formatKRW } from "@/shared/lib";
import { useAdminRefundPolling } from "@/features/admin-refund/useAdminRefundPolling";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function PassActionPanel({ adminKey, onAuthError }: Props) {
  const toast = useToast();
  const { trackRefund } = useAdminRefundPolling(adminKey, onAuthError);
  const [passId, setPassId] = useState("");
  const [actionError, setActionError] = useState<Error | null>(null);

  const expire = useAdminMutation(onAuthError, {
    mutationFn: () => expirePasses(adminKey),
    onMutate: () => setActionError(null),
    onSuccess: (r) => toast.show(`만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`),
    onError: setActionError,
  });

  const refund = useAdminMutation(onAuthError, {
    mutationFn: () => refundPass(adminKey, Number(passId)),
    onMutate: () => setActionError(null),
    onSuccess: (r) => {
      if (r.refundId != null && r.refundStatus != null) {
        toast.show(
          `환불 요청 접수: ${r.refundCredits}회분 ${formatKRW(r.refundAmount)}, 취소 예약 ${r.canceledBookings}건`,
          "info",
        );
        trackRefund(r.refundId, `8회권 #${passId}`);
      } else {
        toast.show(`8회권 정산 완료: 환불 금액 없음, 취소 예약 ${r.canceledBookings}건`);
      }
    },
    onError: setActionError,
  });

  const pending = expire.isPending || refund.isPending;

  return (
    <Card>
      <Card.Header>8회권 관리</Card.Header>
      <Card.Body>
        <ErrorAlert error={actionError} />

        <Row className="g-2 mb-3">
          <Col xs={8}>
            <Form.Group>
              <Form.Label>8회권 ID</Form.Label>
              <Form.Control type="number" min={1} value={passId}
                onChange={(e) => setPassId(e.target.value)} placeholder="환불할 8회권 ID" />
            </Form.Group>
          </Col>
          <Col xs={4} className="d-flex align-items-end">
            <Button variant="danger" className="w-100"
              disabled={!Number(passId) || pending}
              onClick={() => refund.mutate()}>전체 환불</Button>
          </Col>
        </Row>

        <Button variant="outline-secondary" size="sm" disabled={pending}
          onClick={() => expire.mutate()}>
          {expire.isPending ? "실행 중..." : "만료 배치 실행"}
        </Button>
      </Card.Body>
    </Card>
  );
}
