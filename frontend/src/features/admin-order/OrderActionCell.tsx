import { useId, useState } from "react";
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
  const titleId = `admin-order-action-title-${useId()}`;
  const [show, setShow] = useState(false);

  return (
    <>
      <Button
        size="sm"
        variant="outline-danger"
        disabled={disabled}
        onClick={() => setShow(true)}
      >
        {pending ? "처리 중..." : buttonLabel}
      </Button>
      <Modal
        show={show}
        aria-labelledby={titleId}
        onHide={() => !pending && setShow(false)}
        centered
      >
        <Modal.Header closeButton={!pending}>
          <Modal.Title id={titleId} className="fs-6">{title}</Modal.Title>
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
            {pending ? "처리 중..." : "승인"}
          </Button>
          <RiskActionButton
            buttonLabel="거절"
            confirmLabel="거절 및 환불 요청"
            title="주문 거절 영향 확인"
            impact="주문을 거절하면 확보한 상품 재고를 복구하고 전액 환불 요청을 접수합니다. 주문 상태 변경은 즉시 반영되지만 결제사 환불 완료 여부는 별도로 확인해야 합니다."
            disabled={disabled}
            pending={pending}
            onConfirm={() => mutations.reject.mutate(orderId)}
          />
          <Button size="sm" variant="outline-warning" disabled={disabled}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 지연 동의 요청"}
          </Button>
        </div>
      );
    case "IN_PRODUCTION":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="info" disabled={disabled}
            onClick={() => mutations.completeProduction.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 완료"}
          </Button>
          <Button size="sm" variant="outline-warning" disabled={disabled}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 지연 동의 요청"}
          </Button>
          {fulfillmentType === "SHIPPING" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="date" value={shipDateValue}
                aria-label="예상 출고일"
                onChange={(e) => setShipDateValue(e.target.value)}
                style={{ maxWidth: 150 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => mutations.shipDate.mutate({ id: orderId, body: { expectedShipDate: shipDateValue || undefined } })}>출고일 저장</Button>
            </InputGroup>
          )}
        </div>
      );
    case "DELAY_ACCEPTED":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <Button size="sm" variant="outline-success" disabled={disabled}
            onClick={() => mutations.resumeAfterDelay.mutate(orderId)}>
            {pending ? "처리 중..." : "주문 처리 계속"}
          </Button>
          {fulfillmentType === "SHIPPING" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="date" value={shipDateValue}
                aria-label="예상 출고일"
                onChange={(e) => setShipDateValue(e.target.value)}
                style={{ maxWidth: 150 }} />
              <Button variant="outline-primary" disabled={disabled}
                onClick={() => mutations.shipDate.mutate({
                  id: orderId,
                  body: { expectedShipDate: shipDateValue || undefined },
                })}>
                출고일 저장
              </Button>
            </InputGroup>
          )}
        </div>
      );
    case "DELAY_CONSENT_PENDING":
      return (
        <RiskActionButton
          buttonLabel="고객 거절 주문 취소"
          confirmLabel="주문 취소 및 환불 요청"
          title="지연 제안 거절 영향 확인"
          impact="고객이 제작 지연 제안을 거절한 주문을 취소하고 전액 환불 요청을 접수합니다. 확보한 재고는 복구되며 결제사 환불 완료 여부는 별도로 확인해야 합니다."
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
                aria-label="매장 수령 마감 시각"
                onChange={(e) => setPickupDeadline(e.target.value)}
                required
                style={{ maxWidth: 200 }} />
              <Button variant="outline-primary" disabled={disabled || !pickupDeadlineIsFuture}
                onClick={() => mutations.pickup.mutate({ id: orderId, body: { pickupDeadlineAt: pickupDeadline } })}>
                {pending ? "처리 중..." : "매장 수령 준비 완료"}
              </Button>
            </InputGroup>
          )}
          {fulfillmentType === "SHIPPING" && (
            <Button size="sm" variant="outline-info" disabled={disabled}
              onClick={() => mutations.prepareShipping.mutate(orderId)}>
              {pending ? "처리 중..." : "배송 준비"}
            </Button>
          )}
        </div>
      );
    case "SHIPPING_PREPARING":
      return (
        <div className="d-flex gap-1 flex-wrap">
          <InputGroup size="sm" style={{ width: "auto" }}>
            <Form.Control type="date" value={shipDateValue}
              aria-label="예상 출고일"
              onChange={(e) => setShipDateValue(e.target.value)}
              style={{ maxWidth: 150 }} />
            <Button variant="outline-primary" disabled={disabled}
              onClick={() => mutations.shipDate.mutate({ id: orderId, body: { expectedShipDate: shipDateValue || undefined } })}>
              출고일 저장
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
            {pending ? "처리 중..." : "배송 출발"}
          </Button>
        </div>
      );
    case "SHIPPED":
      return (
        <Button size="sm" variant="success" disabled={disabled}
          onClick={() => mutations.delivered.mutate(orderId)}>
          {pending ? "처리 중..." : "배송 완료로 표시"}
        </Button>
      );
    case "PICKUP_READY":
      return (
        <Button size="sm" variant="outline-success" disabled={disabled}
          onClick={() => mutations.pickupDone.mutate(orderId)}>
          {pending ? "처리 중..." : "매장 수령 완료로 표시"}
        </Button>
      );
    default:
      return null;
  }
}
