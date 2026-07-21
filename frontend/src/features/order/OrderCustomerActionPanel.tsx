import { Button } from "react-bootstrap";
import type { OrderDelayDecision, OrderStatus } from "@/shared/types";

interface Props {
  status: OrderStatus;
  pending: boolean;
  onCancel: () => void;
  onDelayDecision: (decision: OrderDelayDecision) => void;
}

export function OrderCustomerActionPanel({
  status,
  pending,
  onCancel,
  onDelayDecision,
}: Props) {
  if (status === "PAID_APPROVAL_PENDING") {
    return (
      <div className="d-flex justify-content-end mt-3">
        <Button variant="outline-danger" disabled={pending}
          onClick={() => window.confirm("주문을 취소하고 환불을 요청하시겠습니까?") && onCancel()}>
          {pending ? "처리 중..." : "주문 취소"}
        </Button>
      </div>
    );
  }

  if (status === "DELAY_CONSENT_PENDING") {
    return (
      <div className="d-flex justify-content-end gap-2 mt-3">
        <Button variant="outline-danger" disabled={pending}
          onClick={() => window.confirm("지연 제안을 거절하고 주문을 환불하시겠습니까?")
            && onDelayDecision("REJECT")}>
          {pending ? "처리 중..." : "지연 거절"}
        </Button>
        <Button variant="primary" disabled={pending}
          onClick={() => onDelayDecision("ACCEPT")}>
          {pending ? "처리 중..." : "지연 수락"}
        </Button>
      </div>
    );
  }

  return null;
}
