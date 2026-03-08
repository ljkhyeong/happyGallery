import { Badge } from "react-bootstrap";

const VARIANT_MAP: Record<string, string> = {
  // Booking
  BOOKED: "primary",
  CANCELED: "secondary",
  NO_SHOW: "danger",
  COMPLETED: "success",
  // Order
  PAID_APPROVAL_PENDING: "warning",
  APPROVED_FULFILLMENT_PENDING: "info",
  REJECTED_REFUNDED: "secondary",
  AUTO_REFUNDED_TIMEOUT: "secondary",
  IN_PRODUCTION: "info",
  DELAY_REQUESTED: "warning",
  PICKUP_READY: "info",
  PICKED_UP: "success",
  PICKUP_EXPIRED_REFUNDED: "secondary",
};

const LABEL_MAP: Record<string, string> = {
  BOOKED: "예약됨",
  CANCELED: "취소됨",
  NO_SHOW: "결석",
  COMPLETED: "완료",
  PAID_APPROVAL_PENDING: "승인 대기",
  APPROVED_FULFILLMENT_PENDING: "이행 대기",
  REJECTED_REFUNDED: "거절/환불",
  AUTO_REFUNDED_TIMEOUT: "자동 환불",
  IN_PRODUCTION: "제작 중",
  DELAY_REQUESTED: "지연 요청",
  PICKUP_READY: "픽업 대기",
  PICKED_UP: "수령 완료",
  PICKUP_EXPIRED_REFUNDED: "미수령 환불",
};

interface Props {
  status: string;
}

export function StatusBadge({ status }: Props) {
  return (
    <Badge bg={VARIANT_MAP[status] ?? "secondary"} className="badge-status">
      {LABEL_MAP[status] ?? status}
    </Badge>
  );
}
