import { useId, useState } from "react";
import { Alert, Button, Form, InputGroup, Modal } from "react-bootstrap";
import type { OrderStatus } from "@/shared/types";
import { parseApiDateTime } from "@/shared/lib";
import type { OrderMutations } from "./useOrderMutations";
import {
  MarkShippedRequestCarrierCode,
  type MarkShippedRequestCarrierCode as GeneratedShippingCarrierCode,
} from "@/generated/api/adminOrder";

type ShippingCarrierCode = NonNullable<GeneratedShippingCarrierCode>;

const SHIPPING_CARRIERS: ReadonlyArray<{ code: ShippingCarrierCode; name: string }> = [
  { code: MarkShippedRequestCarrierCode.CJ_LOGISTICS, name: "CJ대한통운" },
  { code: MarkShippedRequestCarrierCode.LOTTE, name: "롯데택배" },
  { code: MarkShippedRequestCarrierCode.HANJIN, name: "한진택배" },
  { code: MarkShippedRequestCarrierCode.KOREA_POST, name: "우체국택배" },
  { code: MarkShippedRequestCarrierCode.KYUNGDONG, name: "경동택배" },
  { code: MarkShippedRequestCarrierCode.DAESIN, name: "대신택배" },
  { code: MarkShippedRequestCarrierCode.LOGEN, name: "로젠택배" },
  { code: MarkShippedRequestCarrierCode.HAPDONG, name: "합동택배" },
  { code: MarkShippedRequestCarrierCode.COUPANG, name: "쿠팡로지스틱스" },
  { code: MarkShippedRequestCarrierCode.WOORI, name: "우리택배" },
  { code: MarkShippedRequestCarrierCode.CU_POST, name: "CU 편의점택배" },
  { code: MarkShippedRequestCarrierCode.GS_POSTBOX, name: "GS Postbox" },
];

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
  const [carrierCode, setCarrierCode] = useState<ShippingCarrierCode | "">("");
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
          <Form.Select size="sm" aria-label="택배사"
            value={carrierCode ?? ""}
            onChange={(e) => setCarrierCode(e.target.value as ShippingCarrierCode | "")}
            style={{ width: 160 }}>
            <option value="">택배사 선택</option>
            {SHIPPING_CARRIERS.map((candidate) => (
              <option key={candidate.code} value={candidate.code}>{candidate.name}</option>
            ))}
          </Form.Select>
          <Form.Control size="sm" aria-label="운송장 번호" placeholder="운송장 번호"
            value={trackingNumber} onChange={(e) => setTrackingNumber(e.target.value)}
            style={{ width: 170 }} />
          <Button size="sm" variant="primary"
            disabled={disabled || !carrierCode || !trackingNumber.trim()}
            onClick={() => {
              const selectedCarrier = SHIPPING_CARRIERS.find((item) => item.code === carrierCode);
              if (!selectedCarrier || !carrierCode) return;
              mutations.shipped.mutate({
                id: orderId,
                body: {
                  carrier: selectedCarrier.name,
                  carrierCode,
                  trackingNumber: trackingNumber.trim(),
                },
              });
            }}>
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
    case "PICKUP_FORFEITED":
      return (
        <RiskActionButton
          buttonLabel="관리자 예외 환불"
          confirmLabel="전액 환불 요청"
          title="미수령 주문 예외 환불 확인"
          impact="미수령으로 종료된 주문의 전액 환불을 요청합니다. 기성품 재고는 만료 처리 때 이미 복구됐고 주문제작 재고는 판매 재고가 아니므로 재고 수량은 변경하지 않습니다. 주문 상태는 즉시 바뀌지만 결제사 환불 완료 여부는 별도로 확인해야 합니다."
          disabled={disabled}
          pending={pending}
          onConfirm={() => mutations.missedPickupRefund.mutate(orderId)}
        />
      );
    default:
      return null;
  }
}
