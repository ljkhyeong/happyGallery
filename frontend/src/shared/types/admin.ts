import type { AdminRefundStatus } from "./refund";

export type OrderStatus =
  | "PAID_APPROVAL_PENDING"
  | "APPROVED_FULFILLMENT_PENDING"
  | "REJECTED"
  | "CUSTOMER_CANCELED"
  | "AUTO_REFUND_TIMEOUT"
  | "IN_PRODUCTION"
  | "DELAY_CONSENT_PENDING"
  | "DELAY_ACCEPTED"
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

export type BulkSlotStatus =
  | "CREATABLE"
  | "CREATED"
  | "SKIPPED_DUPLICATE"
  | "SKIPPED_PAST";

export interface BulkSlotRequest {
  classId: number;
  dateFrom: string;
  dateTo: string;
  weekdays: string[];
  startTimes: string[];
}

export interface BulkSlotResponse {
  totalCount: number;
  creatableCount: number;
  createdCount: number;
  skippedCount: number;
  items: Array<{
    slotId: number | null;
    startAt: string;
    endAt: string;
    status: BulkSlotStatus;
    bufferBlocked: boolean;
  }>;
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

export interface BookingSettlementResponse {
  bookingId: number;
  status: string;
  balanceStatus: "UNPAID" | "PAID";
  balancePaidAt: string | null;
  arrears: boolean;
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

export interface FailedNotificationResponse {
  outboxId: number;
  recipientType: "USER" | "GUEST";
  recipientId: number;
  eventType: string;
  aggregateType: string | null;
  aggregateId: number | null;
  status: "FAILED" | "PENDING";
  attemptCount: number;
  lastError: string | null;
  createdAt: string;
}

export interface PaymentReconciliationRequiredResponse {
  attemptId: number;
  context: "ORDER" | "BOOKING" | "PASS";
  amount: number;
  status: "RECONCILIATION_REQUIRED";
  reason: string | null;
  createdAt: string;
}

export interface PaymentReconciliationResultResponse {
  attemptId: number;
  status: "RECONCILIATION_REQUIRED" | "CONFIRMED" | "FAILED";
  domainId: number | null;
  message: string;
}

export interface AdminOrderResponse {
  orderId: number;
  orderNumber: string;
  status: OrderStatus;
  totalAmount: number;
  shippingFee: number;
  fulfillmentType: "SHIPPING" | "PICKUP" | null;
  items: Array<{
    productId: number;
    productName: string;
    qty: number;
    unitPrice: number;
  }>;
  paidAt: string | null;
  approvalDeadlineAt: string | null;
  createdAt: string;
}

export interface AdminOrderFulfillmentResponse {
  orderId: number;
  type: "SHIPPING" | "PICKUP";
  shippingAddress: {
    recipientName: string;
    phone: string;
    postalCode: string;
    addressLine1: string;
    addressLine2: string | null;
  } | null;
  expectedShipDate: string | null;
  pickupDeadlineAt: string | null;
  carrier: string | null;
  trackingNumber: string | null;
}

export interface ShippingResponse {
  orderId: number;
  status: OrderStatus;
  expectedShipDate: string | null;
  carrier: string | null;
  trackingNumber: string | null;
}

export interface MarkShippedRequest {
  carrier: string;
  trackingNumber: string;
}

export type OrderApprovalDecision =
  | "APPROVE"
  | "REJECT"
  | "CUSTOMER_CANCEL"
  | "DELAY"
  | "DELAY_ACCEPT"
  | "DELAY_REJECT"
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
  depositPaidAt: string | null;
  balanceAmount: number;
  balanceStatus: "UNPAID" | "PAID";
  balancePaidAt: string | null;
  arrears: boolean;
  passBooking: boolean;
}
