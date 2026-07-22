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
  CUSTOMER_CANCELED: "secondary",
  AUTO_REFUND_TIMEOUT: "secondary",
  IN_PRODUCTION: "info",
  DELAY_CONSENT_PENDING: "warning",
  DELAY_ACCEPTED: "warning",
  DELAY_REJECTED_CANCELED: "secondary",
  PICKUP_READY: "info",
  PICKED_UP: "success",
  PICKUP_EXPIRED: "secondary",
  PICKUP_FORFEITED: "secondary",
  SHIPPING_PREPARING: "info",
  SHIPPED: "primary",
  DELIVERED: "success",
  // Payment
  READY: "secondary",
  CONFIRMING: "info",
  RETRYABLE: "warning",
  FAILED: "danger",
  REVIEW_REQUIRED: "warning",
  REFUNDING: "info",
  REFUNDED: "success",
  SUPPORT_REQUIRED: "warning",
  EXPIRED: "secondary",
  // Pass
  ACTIVE: "success",
  USED_UP: "secondary",
  REFUND_PENDING: "warning",
  REFUND_FAILED: "danger",
};

const LABEL_MAP: Record<string, string> = {
  BOOKED: "예약됨",
  CANCELED: "취소됨",
  NO_SHOW: "결석",
  COMPLETED: "완료",
  PAID_APPROVAL_PENDING: "승인 대기",
  APPROVED_FULFILLMENT_PENDING: "이행 대기",
  REJECTED: "거절",
  CUSTOMER_CANCELED: "고객 취소",
  AUTO_REFUND_TIMEOUT: "자동 환불",
  IN_PRODUCTION: "제작 중",
  DELAY_CONSENT_PENDING: "지연 응답 대기",
  DELAY_ACCEPTED: "지연 수락",
  DELAY_REJECTED_CANCELED: "지연 거절 취소",
  PICKUP_READY: "픽업 대기",
  PICKED_UP: "수령 완료",
  PICKUP_EXPIRED: "미수령 환불",
  PICKUP_FORFEITED: "미수령 종료",
  SHIPPING_PREPARING: "배송 준비",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
  READY: "결제 준비",
  CONFIRMING: "확인 중",
  RETRYABLE: "재확인 필요",
  FAILED: "승인 실패",
  REVIEW_REQUIRED: "결제사 확인",
  REFUNDING: "환불 중",
  REFUNDED: "환불 완료",
  SUPPORT_REQUIRED: "확인 필요",
  EXPIRED: "만료",
  // Pass
  ACTIVE: "사용 가능",
  USED_UP: "모두 사용",
  REFUND_PENDING: "환불 처리 중",
  REFUND_FAILED: "환불 확인 필요",
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
