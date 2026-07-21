import type { RefundStatus } from "./refund";

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
