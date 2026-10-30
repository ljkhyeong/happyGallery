import { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import type { OrderStatus } from "@/shared/types";
import { parseApiDateTime } from "@/shared/lib";
import type { OrderMutations } from "./useOrderMutations";
import { RiskActionButton } from "./RiskActionButton";
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

function ShipDateInput({
  value,
  onChange,
  disabled,
  onSave,
}: {
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
  onSave: () => void;
}) {
  return (
    <InputGroup size="sm" style={{ width: "auto" }}>
      <Form.Control type="date" value={value}
        aria-label="예상 출고일"
        onChange={(e) => onChange(e.target.value)}
        style={{ maxWidth: 150 }} />
      <Button variant="outline-primary" disabled={disabled} onClick={onSave}>
        출고일 저장
      </Button>
    </InputGroup>
  );
}

export function OrderActionCell({ orderId, status, fulfillmentType, mutations }: Props) {
  const [pickupDeadline, setPickupDeadline] = useState("");
  const [shipDateValue, setShipDateValue] = useState("");
  const [carrierCode, setCarrierCode] = useState<ShippingCarrierCode | "">("");
  const [trackingNumber, setTrackingNumber] = useState("");

  const pending = mutations.pendingId === orderId;
  const pickupDeadlineIsFuture = pickupDeadline.length > 0
    && parseApiDateTime(pickupDeadline) > Date.now();
  const shipDateInput = (
    <ShipDateInput
      value={shipDateValue}
      onChange={setShipDateValue}
      disabled={pending}
      onSave={() => mutations.shipDate.mutate({
        id: orderId,
        body: { expectedShipDate: shipDateValue || undefined },
      })}
    />
  );

  switch (status) {
    case "PAID_APPROVAL_PENDING":
      return (
        <div className="d-flex gap-1 flex-wrap text-nowrap">
          <Button size="sm" variant="success" disabled={pending}
            onClick={() => mutations.approve.mutate(orderId)}>
            {pending ? "처리 중..." : "승인"}
          </Button>
          <RiskActionButton
            buttonLabel="거절"
            confirmLabel="거절 및 환불 요청"
            title="주문 거절 및 환불 안내"
            impact="주문은 즉시 거절되고 재고가 복구됩니다. 전액 환불을 요청하며, 환불 완료 여부는 별도로 확인해 주세요."
            disabled={pending}
            pending={pending}
            onConfirm={() => mutations.reject.mutate(orderId)}
          />
          <Button size="sm" variant="outline-warning" disabled={pending}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 지연 동의 요청"}
          </Button>
        </div>
      );
    case "IN_PRODUCTION":
      return (
        <div className="d-flex gap-1 flex-wrap text-nowrap">
          <Button size="sm" variant="info" disabled={pending}
            onClick={() => mutations.completeProduction.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 완료"}
          </Button>
          <Button size="sm" variant="outline-warning" disabled={pending}
            onClick={() => mutations.delay.mutate(orderId)}>
            {pending ? "처리 중..." : "제작 지연 동의 요청"}
          </Button>
          {fulfillmentType === "SHIPPING" && shipDateInput}
        </div>
      );
    case "DELAY_ACCEPTED":
      return (
        <div className="d-flex gap-1 flex-wrap text-nowrap">
          <Button size="sm" variant="outline-success" disabled={pending}
            onClick={() => mutations.resumeAfterDelay.mutate(orderId)}>
            {pending ? "처리 중..." : "주문 처리 계속"}
          </Button>
          {fulfillmentType === "SHIPPING" && shipDateInput}
        </div>
      );
    case "DELAY_CONSENT_PENDING":
      return (
        <RiskActionButton
          buttonLabel="고객 거절 주문 취소"
          confirmLabel="주문 취소 및 환불 요청"
          title="주문 취소 및 환불 안내"
          impact="지연 제안을 거절한 주문을 취소하고 재고를 복구합니다. 전액 환불을 요청하며, 환불 완료 여부는 별도로 확인해 주세요."
          disabled={pending}
          pending={pending}
          onConfirm={() => mutations.delayCancel.mutate(orderId)}
        />
      );
    case "APPROVED_FULFILLMENT_PENDING":
      return (
        <div className="d-flex gap-1 flex-wrap text-nowrap">
          {fulfillmentType === "PICKUP" && (
            <InputGroup size="sm" style={{ width: "auto" }}>
              <Form.Control type="datetime-local" value={pickupDeadline}
                aria-label="매장 수령 마감 시각"
                onChange={(e) => setPickupDeadline(e.target.value)}
                required
                style={{ maxWidth: 200 }} />
              <Button variant="outline-primary" disabled={pending || !pickupDeadlineIsFuture}
                onClick={() => mutations.pickup.mutate({ id: orderId, body: { pickupDeadlineAt: pickupDeadline } })}>
                {pending ? "처리 중..." : "매장 수령 준비 완료"}
              </Button>
            </InputGroup>
          )}
          {fulfillmentType === "SHIPPING" && (
            <Button size="sm" variant="outline-info" disabled={pending}
              onClick={() => mutations.prepareShipping.mutate(orderId)}>
              {pending ? "처리 중..." : "배송 준비"}
            </Button>
          )}
        </div>
      );
    case "SHIPPING_PREPARING":
      return (
        <div className="d-flex gap-1 flex-wrap text-nowrap">
          {shipDateInput}
          <Form.Select size="sm" aria-label="택배사"
            value={carrierCode}
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
            disabled={pending || !carrierCode || !trackingNumber.trim()}
            onClick={() => {
              if (!carrierCode) return;
              mutations.shipped.mutate({
                id: orderId,
                body: { carrierCode, trackingNumber: trackingNumber.trim() },
              });
            }}>
            {pending ? "처리 중..." : "배송 출발"}
          </Button>
        </div>
      );
    case "SHIPPED":
      return (
        <Button size="sm" variant="success" className="text-nowrap" disabled={pending}
          onClick={() => mutations.delivered.mutate(orderId)}>
          {pending ? "처리 중..." : "배송 완료로 표시"}
        </Button>
      );
    case "PICKUP_READY":
      return (
        <Button size="sm" variant="outline-success" className="text-nowrap" disabled={pending}
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
          impact="미수령 주문의 전액 환불을 요청합니다. 기성품 재고는 이미 복구됐으며 주문제작 재고는 변경하지 않습니다. 주문 상태는 즉시 바뀌고, 환불 완료 여부는 별도로 확인해 주세요."
          disabled={pending}
          pending={pending}
          onConfirm={() => mutations.missedPickupRefund.mutate(orderId)}
        />
      );
    default:
      return null;
  }
}
