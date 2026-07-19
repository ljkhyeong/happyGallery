export interface OffsetPage<T> {
  content: T[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

export interface AdminOrderSearchRow {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  buyerName: string;
  buyerPhone: string | null;
  paidAt: string | null;
  approvalDeadlineAt: string | null;
  createdAt: string;
}

export interface AdminBookingSearchRow {
  bookingId: number;
  bookingNumber: string;
  bookerType: "GUEST" | "MEMBER";
  bookerName: string;
  bookerPhone: string | null;
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
  createdAt: string;
}
