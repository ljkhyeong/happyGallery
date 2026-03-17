export type OrderStatus =
  | "PAID_APPROVAL_PENDING"
  | "APPROVED_FULFILLMENT_PENDING"
  | "REJECTED"
  | "AUTO_REFUND_TIMEOUT"
  | "IN_PRODUCTION"
  | "DELAY_REQUESTED"
  | "SHIPPING_PREPARING"
  | "SHIPPED"
  | "DELIVERED"
  | "PICKUP_READY"
  | "PICKED_UP"
  | "PICKUP_EXPIRED"
  | "COMPLETED";

export interface SlotResponse {
  id: number;
  classId: number;
  startAt: string;
  endAt: string;
  capacity: number;
  bookedCount: number;
  isActive: boolean;
}

export interface CreateSlotRequest {
  classId: number;
  startAt: string;
  endAt: string;
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
  amount: number;
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
  | "AUTO_REFUND"
  | "PRODUCTION_COMPLETE"
  | "RESUME_PRODUCTION"
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
