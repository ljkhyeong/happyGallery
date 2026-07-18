import type { RefundStatus } from "./refund";

export interface PassRefundResponse {
  canceledBookings: number;
  refundCredits: number;
  refundAmount: number;
  refundId: number | null;
  refundStatus: RefundStatus | null;
}
