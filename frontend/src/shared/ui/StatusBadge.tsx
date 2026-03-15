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
  REJECTED: "secondary",
  AUTO_REFUND_TIMEOUT: "secondary",
  IN_PRODUCTION: "info",
  DELAY_REQUESTED: "warning",
  PICKUP_READY: "info",
  PICKED_UP: "success",
  PICKUP_EXPIRED: "secondary",
  SHIPPING_PREPARING: "info",
  SHIPPED: "primary",
  DELIVERED: "success",
};

const LABEL_MAP: Record<string, string> = {
  BOOKED: "예약됨",
  CANCELED: "취소됨",
  NO_SHOW: "결석",
  COMPLETED: "완료",
  PAID_APPROVAL_PENDING: "승인 대기",
  APPROVED_FULFILLMENT_PENDING: "이행 대기",
  REJECTED: "거절",
  AUTO_REFUND_TIMEOUT: "자동 환불",
  IN_PRODUCTION: "제작 중",
  DELAY_REQUESTED: "지연 요청",
  PICKUP_READY: "픽업 대기",
  PICKED_UP: "수령 완료",
  PICKUP_EXPIRED: "미수령 만료",
  SHIPPING_PREPARING: "배송 준비",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
};

export function getStatusLabel(status: string) {
  return LABEL_MAP[status] ?? status;
}

interface Props {
  status: string;
}

export function StatusBadge({ status }: Props) {
  return (
    <Badge bg={VARIANT_MAP[status] ?? "secondary"} className="badge-status">
      {getStatusLabel(status)}
    </Badge>
  );
}
