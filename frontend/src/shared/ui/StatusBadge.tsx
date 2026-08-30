import { Badge } from "react-bootstrap";
import { getStatusLabel, type StatusAudience } from "@/shared/lib/labels";

export { getStatusLabel, type StatusAudience } from "@/shared/lib/labels";

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

interface Props {
  status: string;
  audience?: StatusAudience;
}

export function StatusBadge({ status, audience = "customer" }: Props) {
  return (
    <Badge bg={VARIANT_MAP[status] ?? "secondary"} className="badge-status">
      {getStatusLabel(status, audience)}
    </Badge>
  );
}
