import type { RefundStatus } from "./refund";

export type AdminPassStatus =
  | "ACTIVE"
  | "USED_UP"
  | "EXPIRED"
  | "REFUND_PENDING"
  | "REFUND_FAILED"
  | "REFUNDED";

export interface AdminPassResponse {
  passId: number;
  passNumber: string;
  customerName: string;
  customerPhone: string | null;
  status: AdminPassStatus;
  remainingCredits: number;
  totalCredits: number;
  expiresAt: string;
  futureBookingCount: number;
  expectedRefundAmount: number;
  refundStatus: RefundStatus | null;
}

export interface PassRefundResponse {
  canceledBookings: number;
  refundCredits: number;
  refundAmount: number;
  refundId: number | null;
  refundStatus: RefundStatus | null;
}

export interface MemberPassRefundResponse {
  canceledBookings: number;
  refundCredits: number;
  refundAmount: number;
  refundStatus: RefundStatus | null;
}
