import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Card, Form, Button, Row, Col } from "react-bootstrap";
import { expirePasses, refundPass } from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
}

export function PassActionPanel({ adminKey }: Props) {
  const toast = useToast();
  const [passId, setPassId] = useState("");

  const expire = useMutation({
    mutationFn: () => expirePasses(adminKey),
    onSuccess: (r) => toast.show(`만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`),
  });

  const refund = useMutation({
    mutationFn: () => refundPass(adminKey, Number(passId)),
    onSuccess: (r) =>
      toast.show(`환불 완료: ${r.refundCredits}회분 ${formatKRW(r.refundAmount)}, 취소 예약 ${r.canceledBookings}건`),
  });

  const pending = expire.isPending || refund.isPending;

  return (
    <Card>
      <Card.Header>8회권 관리</Card.Header>
      <Card.Body>
        <ErrorAlert error={expire.error || refund.error} />

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
