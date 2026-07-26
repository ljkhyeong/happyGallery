import { useState } from "react";
import { Alert, Button, Form, InputGroup, Modal } from "react-bootstrap";
import type { OrderStatus } from "@/shared/types";
import { parseApiDateTime } from "@/shared/lib";
import type { OrderMutations } from "./useOrderMutations";

interface Props {
  orderId: number;
  status: OrderStatus;
  fulfillmentType: "SHIPPING" | "PICKUP" | null;
  mutations: OrderMutations;
}

interface RiskActionButtonProps {
  buttonLabel: string;
  confirmLabel: string;
  title: string;
  impact: string;
  disabled: boolean;
  pending: boolean;
  onConfirm: () => void;
}

function RiskActionButton({
  buttonLabel,
  confirmLabel,
  title,
  impact,
  disabled,
  pending,
  onConfirm,
}: RiskActionButtonProps) {
  const [show, setShow] = useState(false);

  return (
    <>
      <Button
        size="sm"
        variant="outline-danger"
        disabled={disabled}
        onClick={() => setShow(true)}
      >
        {pending ? "..." : buttonLabel}
      </Button>
      <Modal show={show} onHide={() => !pending && setShow(false)} centered>
        <Modal.Header closeButton={!pending}>
          <Modal.Title className="fs-6">{title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Alert variant="warning" className="mb-0 small">
            {impact}
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" disabled={pending} onClick={() => setShow(false)}>
            닫기
          </Button>
          <Button
            variant="danger"
            disabled={pending}
            onClick={() => {
              setShow(false);
              onConfirm();
            }}
          >
            {confirmLabel}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}

export function OrderActionCell({ orderId, status, fulfillmentType, mutations }: Props) {
  const [pickupDeadline, setPickupDeadline] = useState("");
  const [shipDateValue, setShipDateValue] = useState("");
  const [carrier, setCarrier] = useState("");
  const [trackingNumber, setTrackingNumber] = useState("");

  const disabled = mutations.pendingId === orderId;
  const pending = disabled;
  const pickupDeadlineIsFuture = pickupDeadline.length > 0
    && parseApiDateTime(pickupDeadline) > Date.now();

  switch (status) {
    case "PAID_APPROVAL_PENDING":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="success" disabled={disabled}
            onClick={() => mutations.approve.mutate(orderId)}>
            {pending ? "..." : "승인"}
          </Button>
          <RiskActionButton
            buttonLabel="거절"
            confirmLabel="거절 및 환불 요청"
            title="주문 거절 영향 확인"
            impact="주문을 거절하면 확보한 상품 재고를 복구하고 전액 환불 요청을 접수합니다. 주문 상태 변경은 즉시 반영되지만 PG 환불 완료는 별도로 확인해야 합니다."
            disabled={disabled}
            pending={pending}
            onConfirm={() => mutations.reject.mutate(orderId)}
          />
          <Button size="sm" variant="outline-warning" disabled={disabled}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "..." : "지연 제안"}
          </Button>
        </div>
      );
    case "IN_PRODUCTION":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="info" disabled={disabled}
            onClick={() => mutations.completeProduction.mutate(orderId)}>
            {pending ? "..." : "제작 완료"}
          </Button>
          <Button size="sm" variant="outline-warning" disabled={disabled}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "..." : "지연 제안"}
          </Button>
          {fulfillmentType === "SHIPPING" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="date" value={shipDateValue}
                onChange={(e) => setShipDateValue(e.target.value)}
                style={{ maxWidth: 150 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => mutations.shipDate.mutate({ id: orderId, body: { expectedShipDate: shipDateValue || undefined } })}>출고일</Button>
            </InputGroup>
          )}
        </div>
      );
    case "DELAY_ACCEPTED":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="outline-success" disabled={disabled}
            onClick={() => mutations.resumeAfterDelay.mutate(orderId)}>
            {pending ? "..." : "처리 재개"}
          </Button>
          {fulfillmentType === "SHIPPING" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="date" value={shipDateValue}
                onChange={(e) => setShipDateValue(e.target.value)}
                style={{ maxWidth: 150 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => mutations.shipDate.mutate({
                  id: orderId,
                  body: { expectedShipDate: shipDateValue || undefined },
                })}>
                출고일
              </Button>
            </InputGroup>
          )}
        </div>
      );
    case "DELAY_CONSENT_PENDING":
      return (
        <RiskActionButton
          buttonLabel="거절 처리"
          confirmLabel="거절 취소 및 환불 요청"
          title="지연 제안 거절 영향 확인"
          impact="고객이 지연 제안을 거절한 주문을 취소하고 전액 환불 요청을 접수합니다. 확보한 재고는 복구되며 PG 환불 완료는 별도로 확인해야 합니다."
          disabled={disabled}
          pending={pending}
          onConfirm={() => mutations.delayCancel.mutate(orderId)}
        />
      );
    case "APPROVED_FULFILLMENT_PENDING":
      return (
        <div className="d-flex gap-1 flex-wrap">
          {fulfillmentType === "PICKUP" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="datetime-local" value={pickupDeadline}
                onChange={(e) => setPickupDeadline(e.target.value)}
                required
                style={{ maxWidth: 200 }} />
              <Button variant="outline-primary" disabled={disabled || !pickupDeadlineIsFuture}
                onClick={() => mutations.pickup.mutate({ id: orderId, body: { pickupDeadlineAt: pickupDeadline } })}>
                {pending ? "..." : "픽업 준비"}
              </Button>
            </InputGroup>
          )}
          {fulfillmentType === "SHIPPING" && (
            <Button size="sm" variant="outline-info" disabled={disabled}
              onClick={() => mutations.prepareShipping.mutate(orderId)}>
              {pending ? "..." : "배송 준비"}
            </Button>
          )}
        </div>
      );
    case "SHIPPING_PREPARING":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <InputGroup size="sm" style={{ width: "auto" }}>
            <Form.Control type="date" value={shipDateValue}
              onChange={(e) => setShipDateValue(e.target.value)}
              style={{ maxWidth: 150 }} />
            <Button variant="outline-primary" disabled={disabled}
              onClick={() => mutations.shipDate.mutate({ id: orderId, body: { expectedShipDate: shipDateValue || undefined } })}>
              출고일
            </Button>
          </InputGroup>
          <Form.Control size="sm" aria-label="택배사" placeholder="택배사"
            value={carrier} onChange={(e) => setCarrier(e.target.value)}
            style={{ width: 120 }} />
          <Form.Control size="sm" aria-label="운송장 번호" placeholder="운송장 번호"
            value={trackingNumber} onChange={(e) => setTrackingNumber(e.target.value)}
            style={{ width: 170 }} />
          <Button size="sm" variant="primary"
            disabled={disabled || !carrier.trim() || !trackingNumber.trim()}
            onClick={() => mutations.shipped.mutate({
              id: orderId,
              body: { carrier: carrier.trim(), trackingNumber: trackingNumber.trim() },
            })}>
            {pending ? "..." : "배송 출발"}
          </Button>
        </div>
      );
    case "SHIPPED":
      return (
        <Button size="sm" variant="success" disabled={disabled}
          onClick={() => mutations.delivered.mutate(orderId)}>
          {pending ? "..." : "배송 완료"}
        </Button>
      );
    case "PICKUP_READY":
      return (
        <Button size="sm" variant="outline-success" disabled={disabled}
          onClick={() => mutations.pickupDone.mutate(orderId)}>
          {pending ? "..." : "픽업 완료"}
        </Button>
      );
    default:
      return null;
  }
}
