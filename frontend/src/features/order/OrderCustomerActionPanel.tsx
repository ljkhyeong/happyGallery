import { useState } from "react";
import { Alert, Button } from "react-bootstrap";
import type { OrderDetailResponse } from "@/generated/api/order";
import { formatDate } from "@/shared/lib";
import type { OrderDelayDecision } from "@/shared/types";
import { ConfirmModal, ErrorAlert } from "@/shared/ui";
import { canEditShippingAddress, ShippingAddressEditPanel } from "./ShippingAddressEditPanel";

interface Props {
  order: OrderDetailResponse;
  /** 배송지를 고칠 때 쓰는 요청 정보. 비회원은 조회 토큰을 함께 넘기고, 비우면 배송지 수정 버튼을 두지 않는다. */
  shippingAddressEdit?: { accessToken?: string; onSaved: () => Promise<unknown> };
  pending: boolean;
  error: unknown;
  onCancel: () => void;
  onDelayDecision: (decision: OrderDelayDecision) => void;
}

/**
 * 주문 상세 카드 아래의 고객 행동. 답해야 하는 지연 동의를 먼저 두고, 배송지 수정·주문 취소는 한 줄에 둔다.
 * 배송지를 고치는 동안에는 행동 줄 대신 수정 칸을 연다.
 */
export function OrderCustomerActionPanel({
  order,
  shippingAddressEdit,
  pending,
  error,
  onCancel,
  onDelayDecision,
}: Props) {
  const [confirmAction, setConfirmAction] = useState<"CANCEL" | "REJECT" | null>(null);
  // 저장이나 다른 곳의 변경으로 배송지 버전이 바뀌면 수정 칸을 닫는다.
  const addressVersionKey = `${order.orderId}:${order.fulfillment?.version ?? ""}`;
  const [editingAddressKey, setEditingAddressKey] = useState<string | null>(null);
  const canEditAddress = shippingAddressEdit !== undefined && canEditShippingAddress(order);
  const canCancel = order.status === "PAID_APPROVAL_PENDING";
  const canRespondToDelay = order.status === "DELAY_CONSENT_PENDING";
  const expectedShipDate = order.fulfillment?.expectedShipDate;

  if (!canCancel && !canRespondToDelay && !canEditAddress) return null;

  const confirmTitle = confirmAction === "REJECT"
    ? "변경된 제작 일정 거절 및 환불 요청"
    : "주문 취소 및 환불 요청";
  const confirmMessage = confirmAction === "REJECT"
    ? "공방이 제안한 변경 일정을 거절하면 주문이 취소되고 전액 환불을 요청합니다."
    : "주문을 취소하면 전액 환불을 요청합니다.";
  const confirmLabel = confirmAction === "REJECT"
    ? "거절하고 환불 요청"
    : "취소하고 환불 요청";

  const submitConfirmedAction = () => {
    if (confirmAction === "REJECT") {
      onDelayDecision("REJECT");
    } else if (confirmAction === "CANCEL") {
      onCancel();
    }
  };

  return (
    <>
      {canRespondToDelay && (
        // 고객이 답해야 하는 일이라 노랑 안내 상자 안에 요청 내용과 두 버튼을 함께 둔다(응답 기한은 서버가 주지 않아 쓰지 않는다).
        <Alert variant="warning" className="mb-3" aria-labelledby="order-delay-consent-title">
          <h2 id="order-delay-consent-title" className="h6 mb-2">주문 처리 일정 변경 요청</h2>
          <p className="mb-2">공방이 주문 처리 일정을 늦추자고 요청했습니다.</p>
          {expectedShipDate && (
            <p className="mb-2">예상 출고일 <strong>{formatDate(expectedShipDate)}</strong></p>
          )}
          <p className="small mb-3">
            동의하면 바뀐 일정으로 주문을 이어 갑니다. 거절하면 주문이 취소되고 전액 환불을 요청합니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button
              variant="dark"
              disabled={pending}
              onClick={() => onDelayDecision("ACCEPT")}
            >
              {pending ? "처리 중..." : "변경 일정에 동의"}
            </Button>
            <Button
              variant="outline-dark"
              disabled={pending}
              onClick={() => setConfirmAction("REJECT")}
            >
              {pending ? "처리 중..." : "변경 일정 거절"}
            </Button>
          </div>
        </Alert>
      )}

      {canEditAddress && editingAddressKey === addressVersionKey ? (
        <ShippingAddressEditPanel
          order={order}
          accessToken={shippingAddressEdit.accessToken}
          onSaved={shippingAddressEdit.onSaved}
          onClose={() => setEditingAddressKey(null)}
        />
      ) : (canEditAddress || canCancel) && (
        <div className="d-flex flex-wrap gap-2 mb-3">
          {canEditAddress && (
            <Button variant="outline-dark" onClick={() => setEditingAddressKey(addressVersionKey)}>
              배송지 수정
            </Button>
          )}
          {canCancel && (
            <Button
              variant="outline-dark"
              disabled={pending}
              onClick={() => setConfirmAction("CANCEL")}
            >
              {pending ? "처리 중..." : "주문 취소"}
            </Button>
          )}
        </div>
      )}

      <ConfirmModal
        show={confirmAction !== null}
        title={confirmTitle}
        pending={pending}
        onClose={() => setConfirmAction(null)}
        confirmLabel={pending ? "처리 중..." : confirmLabel}
        confirmVariant="dark"
        onConfirm={submitConfirmedAction}
        cancelLabel="돌아가기"
        cancelVariant="outline-dark"
        backdrop={pending ? "static" : true}
        keyboard={!pending}
      >
        <ErrorAlert error={error} />
        <p>{confirmMessage}</p>
        <Alert variant="info" className="mb-0 py-2 small">
          주문 취소는 바로 반영되며, 실제 환불 완료 시점은 결제사에 따라 달라질 수 있습니다.
        </Alert>
      </ConfirmModal>
    </>
  );
}
