import type { AdminRefundStatus } from "./refund";

export type OrderStatus =
  | "PAID_APPROVAL_PENDING"
  | "APPROVED_FULFILLMENT_PENDING"
  | "REJECTED"
  | "AUTO_REFUND_TIMEOUT"
  | "IN_PRODUCTION"
  | "DELAY_REQUESTED"
  | "DELAY_REJECTED_CANCELED"
  | "SHIPPING_PREPARING"
  | "SHIPPED"
  | "DELIVERED"
  | "PICKUP_READY"
  | "PICKED_UP"
  | "PICKUP_EXPIRED"
  | "PICKUP_FORFEITED"
  | "COMPLETED";

export interface SlotResponse {
  id: number;
  classId: number;
  startAt: string;
  endAt: string;
  capacity: number;
  bookedCount: number;
  adminActive: boolean;
  bufferBlocked: boolean;
  isActive: boolean;
}

export interface CreateSlotRequest {
  classId: number;
  startAt: string;
}

export interface BatchResponse {
  successCount: number;
  failureCount: number;
  failureReasons: Record<string, number>;
}

export interface BookingNoShowResponse {
  bookingId: number;
  status: string;
}

export interface OrderProductionResponse {
  orderId: number;
  status: OrderStatus;
  expectedShipDate: string | null;
}

export interface OrderRejectResponse {
  orderId: number;
  orderStatus: OrderStatus;
  refund: AdminRefundStatus;
}

export interface OrderDelayCancellationResponse {
  orderId: number;
  orderStatus: OrderStatus;
  expectedShipDate: string | null;
  refund: AdminRefundStatus;
}

export interface PickupResponse {
  orderId: number;
  status: OrderStatus;
  pickupDeadlineAt: string | null;
}

export interface MarkPickupReadyRequest {
  pickupDeadlineAt?: string;
}

export interface SetExpectedShipDateRequest {
  expectedShipDate?: string;
}

export interface FailedRefundResponse {
  refundId: number;
  bookingId: number | null;
  orderId: number | null;
  passPurchaseId: number | null;
  paymentAttemptId: number | null;
  amount: number;
  status: "FAILED" | "RETRYABLE" | "RECONCILIATION_REQUIRED";
  attemptCount: number;
  failReason: string;
  createdAt: string;
}

export interface AdminOrderResponse {
  orderId: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  paidAt: string | null;
  approvalDeadlineAt: string | null;
  createdAt: string;
}

export interface ShippingResponse {
  orderId: number;
  status: OrderStatus;
  expectedShipDate: string | null;
}

export type OrderApprovalDecision =
  | "APPROVE"
  | "REJECT"
  | "DELAY"
  | "DELAY_CANCEL"
  | "AUTO_REFUND"
  | "PRODUCTION_COMPLETE"
  | "RESUME_PRODUCTION"
  | "PICKUP_READY"
  | "PICKUP_COMPLETE"
  | "PICKUP_EXPIRED"
  | "PICKUP_FORFEITED"
  | "PREPARE_SHIPPING"
  | "SHIP"
  | "DELIVER";

export interface OrderHistoryResponse {
  id: number;
  decision: OrderApprovalDecision;
  decidedByAdminId: number | null;
  reason: string | null;
  decidedAt: string;
}

export interface AdminBookingResponse {
  bookingId: number;
  bookingNumber: string;
  bookerType: "GUEST" | "MEMBER";
  bookerName: string;
  bookerPhone: string;
  className: string;
  startAt: string;
  endAt: string;
  status: string;
  depositAmount: number;
  balanceAmount: number;
  passBooking: boolean;
}
