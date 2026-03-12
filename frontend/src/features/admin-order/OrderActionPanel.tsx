import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Card, Form, Button, Row, Col, InputGroup } from "react-bootstrap";
import {
  approveOrder, rejectOrder, completeProduction,
  setExpectedShipDate, requestDelay,
  preparePickup, completePickup, expirePickups,
} from "./api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function OrderActionPanel({ adminKey, onAuthError }: Props) {
  const toast = useToast();
  const [orderId, setOrderId] = useState("");
  const [shipDate, setShipDate] = useState("");
  const [pickupDeadline, setPickupDeadline] = useState("");

  const id = Number(orderId);

  function onError(error: Error) {
    if (error instanceof ApiError && error.status === 401) onAuthError();
  }

  const approve = useMutation({
    mutationFn: () => approveOrder(adminKey, id),
    onSuccess: () => toast.show(`주문 #${id} 승인 완료`),
    onError,
  });
  const reject = useMutation({
    mutationFn: () => rejectOrder(adminKey, id),
    onSuccess: () => toast.show(`주문 #${id} 거절 완료`),
    onError,
  });
  const complete = useMutation({
    mutationFn: () => completeProduction(adminKey, id),
    onSuccess: () => toast.show(`주문 #${id} 제작 완료`),
    onError,
  });
  const setShip = useMutation({
    mutationFn: () => setExpectedShipDate(adminKey, id, { expectedShipDate: shipDate || undefined }),
    onSuccess: () => toast.show(`주문 #${id} 출고일 설정`),
    onError,
  });
  const delay = useMutation({
    mutationFn: () => requestDelay(adminKey, id),
    onSuccess: () => toast.show(`주문 #${id} 지연 요청`),
    onError,
  });
  const pickup = useMutation({
    mutationFn: () => preparePickup(adminKey, id, { pickupDeadlineAt: pickupDeadline || undefined }),
    onSuccess: () => toast.show(`주문 #${id} 픽업 준비 완료`),
    onError,
  });
  const pickupDone = useMutation({
    mutationFn: () => completePickup(adminKey, id),
    onSuccess: () => toast.show(`주문 #${id} 픽업 완료`),
    onError,
  });
  const expire = useMutation({
    mutationFn: () => expirePickups(adminKey),
    onSuccess: (r) => toast.show(`픽업 만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`),
    onError,
  });

  const pending = approve.isPending || reject.isPending || complete.isPending
    || setShip.isPending || delay.isPending || pickup.isPending
    || pickupDone.isPending || expire.isPending;

  const lastError = approve.error || reject.error || complete.error
    || setShip.error || delay.error || pickup.error
    || pickupDone.error || expire.error;

  return (
    <Card>
      <Card.Header>주문 관리</Card.Header>
      <Card.Body>
        <ErrorAlert error={lastError} />

        <Form.Group className="mb-3">
          <Form.Label>주문 ID</Form.Label>
          <Form.Control
            type="number" min={1} value={orderId}
            onChange={(e) => setOrderId(e.target.value)}
            placeholder="주문 ID"
          />
        </Form.Group>

        <Row className="g-2 mb-3">
          <Col xs={6} sm={4} md={3}>
            <Button variant="success" className="w-100" size="sm"
              disabled={!id || pending} onClick={() => approve.mutate()}>승인</Button>
          </Col>
          <Col xs={6} sm={4} md={3}>
            <Button variant="danger" className="w-100" size="sm"
              disabled={!id || pending} onClick={() => reject.mutate()}>거절</Button>
          </Col>
          <Col xs={6} sm={4} md={3}>
            <Button variant="info" className="w-100" size="sm"
              disabled={!id || pending} onClick={() => complete.mutate()}>제작 완료</Button>
          </Col>
          <Col xs={6} sm={4} md={3}>
            <Button variant="warning" className="w-100" size="sm"
              disabled={!id || pending} onClick={() => delay.mutate()}>지연 요청</Button>
          </Col>
        </Row>

        <Row className="g-2 mb-3">
          <Col xs={12} sm={6}>
            <InputGroup size="sm">
              <Form.Control type="date" value={shipDate}
                onChange={(e) => setShipDate(e.target.value)} placeholder="출고일" />
              <Button variant="outline-primary" disabled={!id || pending}
                onClick={() => setShip.mutate()}>출고일 설정</Button>
            </InputGroup>
          </Col>
          <Col xs={12} sm={6}>
            <InputGroup size="sm">
              <Form.Control type="datetime-local" value={pickupDeadline}
                onChange={(e) => setPickupDeadline(e.target.value)} />
              <Button variant="outline-primary" disabled={!id || pending}
                onClick={() => pickup.mutate()}>픽업 준비</Button>
            </InputGroup>
          </Col>
        </Row>

        <Row className="g-2">
          <Col xs={6} sm={4}>
            <Button variant="outline-success" className="w-100" size="sm"
              disabled={!id || pending} onClick={() => pickupDone.mutate()}>픽업 완료</Button>
          </Col>
          <Col xs={6} sm={4}>
            <Button variant="outline-secondary" className="w-100" size="sm"
              disabled={pending} onClick={() => expire.mutate()}>픽업 만료 배치</Button>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
}
