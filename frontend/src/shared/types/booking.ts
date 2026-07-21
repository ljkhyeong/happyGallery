import type { RefundProgress } from "./refund";

export type BookingStatus = "BOOKED" | "CANCELED" | "NO_SHOW" | "COMPLETED";
export type DepositPaymentMethod = "CARD" | "EASY_PAY";

export interface BookingCancelPolicy {
  cancellable: boolean;
  refundable: boolean;
  deadlineAt: string;
  passCreditRestorable: boolean;
  warningCode: string | null;
}

export interface SendVerificationRequest {
  phone: string;
}

export interface SendVerificationResponse {
  verificationId: number;
  phone: string;
}

export interface BookingDetailResponse {
  bookingId: number;
  bookingNumber: string;
  classId: number;
  slotId: number;
  startAt: string;
  endAt: string;
  className: string;
  status: BookingStatus;
  depositAmount: number;
  balanceAmount: number;
  guestName: string;
  guestPhone: string;
  cancelPolicy: BookingCancelPolicy;
  refund: RefundProgress | null;
}

export interface MyBookingDetailResponse {
  bookingId: number;
  classId: number;
  slotId: number;
  startAt: string;
  endAt: string;
  className: string;
  status: BookingStatus;
  depositAmount: number;
  balanceAmount: number;
  balanceStatus: string;
  passBooking: boolean;
  cancelPolicy: BookingCancelPolicy;
  refund: RefundProgress | null;
}

export interface RescheduleResponse {
  bookingId: number;
  bookingNumber: string;
  slotId: number;
  startAt: string;
  endAt: string;
  className: string;
  status: BookingStatus;
}

export interface CancelResponse {
  bookingId: number;
  status: BookingStatus;
  refundable: boolean;
  refundAmount: number;
  refund: RefundProgress | null;
}
