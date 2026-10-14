import { Badge } from "react-bootstrap";
import { getStatusLabel, type StatusAudience } from "@/shared/lib/labels";


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

// 고객 화면은 먹색 하나로 쓴다: 진행 중은 먹색, 끝난 상태는 회색 면, 고객이 확인할 일은 노랑(먹색 글자), 실패만 빨강.
// 관리자 화면은 상태를 빠르게 훑도록 위 색을 그대로 쓴다.
const CUSTOMER_TONE: Record<string, string> = {
  primary: "dark",
  info: "dark",
  success: "light",
  secondary: "light",
};
// 불참은 오류가 아니라 끝난 상태이고, 사용 가능한 이용권은 진행 중인 상태다.
const CUSTOMER_STATUS_TONE: Record<string, string> = {
  NO_SHOW: "light",
  ACTIVE: "dark",
};

interface Props {
  status: string;
  audience?: StatusAudience;
}

export function StatusBadge({ status, audience = "customer" }: Props) {
  const variant = VARIANT_MAP[status] ?? "secondary";
  const bg = audience === "customer"
    ? CUSTOMER_STATUS_TONE[status] ?? CUSTOMER_TONE[variant] ?? variant
    : variant;
  return (
    <Badge bg={bg} text={bg === "light" || bg === "warning" ? "dark" : undefined} className="badge-status">
      {getStatusLabel(status, audience)}
    </Badge>
  );
}
