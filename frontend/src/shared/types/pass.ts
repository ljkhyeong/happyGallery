export interface PurchasePassRequest {
  guestId: number;
  totalPrice?: number;
}

export interface PurchasePassResponse {
  passId: number;
  guestId: number;
  expiresAt: string;
  totalCredits: number;
  remainingCredits: number;
  totalPrice: number;
}

export interface PassRefundResponse {
  canceledBookings: number;
  refundCredits: number;
  refundAmount: number;
}
