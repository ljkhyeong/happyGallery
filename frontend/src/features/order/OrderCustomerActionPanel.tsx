import { useId, useState } from "react";
import { Alert, Button, Modal } from "react-bootstrap";
import type { OrderDelayDecision, OrderStatus } from "@/shared/types";
import { ErrorAlert } from "@/shared/ui";

interface Props {
  status: OrderStatus;
  pending: boolean;
  error: unknown;
  onCancel: () => void;
  onDelayDecision: (decision: OrderDelayDecision) => void;
}

export function OrderCustomerActionPanel({
  status,
  pending,
  error,
  onCancel,
  onDelayDecision,
}: Props) {
  const titleId = `order-customer-action-title-${useId()}`;
  const [confirmAction, setConfirmAction] = useState<"CANCEL" | "REJECT" | null>(null);
  const canCancel = status === "PAID_APPROVAL_PENDING";
  const canRespondToDelay = status === "DELAY_CONSENT_PENDING";

  if (!canCancel && !canRespondToDelay) return null;

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
      {canCancel && (
        <div className="d-flex justify-content-end mt-3">
          <Button
            variant="outline-danger"
            disabled={pending}
            onClick={() => setConfirmAction("CANCEL")}
          >
            {pending ? "처리 중..." : "주문 취소"}
          </Button>
        </div>
      )}

      {canRespondToDelay && (
        <div className="d-flex justify-content-end gap-2 mt-3">
          <Button
            variant="outline-danger"
            disabled={pending}
            onClick={() => setConfirmAction("REJECT")}
          >
            {pending ? "처리 중..." : "변경 일정 거절"}
          </Button>
          <Button
            variant="primary"
            disabled={pending}
            onClick={() => onDelayDecision("ACCEPT")}
          >
            {pending ? "처리 중..." : "변경 일정에 동의"}
          </Button>
        </div>
      )}

      <Modal
        show={confirmAction !== null}
        aria-labelledby={titleId}
        onHide={() => {
          if (!pending) setConfirmAction(null);
        }}
        backdrop={pending ? "static" : true}
        keyboard={!pending}
        centered
      >
        <Modal.Header closeButton={!pending}>
          <Modal.Title id={titleId} className="fs-6">{confirmTitle}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={error} />
          <p>{confirmMessage}</p>
          <Alert variant="info" className="mb-0 py-2 small">
            주문 취소는 바로 반영되며, 실제 환불 완료 시점은 결제사에 따라 달라질 수 있습니다.
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button
            type="button"
            variant="secondary"
            disabled={pending}
            onClick={() => setConfirmAction(null)}
          >
            돌아가기
          </Button>
          <Button
            type="button"
            variant="danger"
            disabled={pending}
            onClick={submitConfirmedAction}
          >
            {pending ? "처리 중..." : confirmLabel}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
