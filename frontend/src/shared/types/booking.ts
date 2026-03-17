export type BookingStatus = "BOOKED" | "CANCELED" | "NO_SHOW" | "COMPLETED";
export type DepositPaymentMethod = "CARD" | "EASY_PAY";

export interface SendVerificationRequest {
  phone: string;
}

export interface SendVerificationResponse {
  verificationId: number;
  phone: string;
}

export interface CreateGuestBookingRequest {
  phone: string;
  verificationCode: string;
  name: string;
  slotId: number;
  depositAmount?: number;
  paymentMethod?: DepositPaymentMethod;
  passId?: number;
}

export interface BookingResponse {
  bookingId: number;
  bookingNumber: string;
  accessToken: string;
  slotId: number;
  status: BookingStatus;
  depositAmount: number;
  balanceAmount: number;
  className: string;
}

export interface BookingDetailResponse {
  bookingId: number;
  bookingNumber: string;
  slotId: number;
  startAt: string;
  endAt: string;
  className: string;
  status: BookingStatus;
  depositAmount: number;
  balanceAmount: number;
  guestName: string;
  guestPhone: string;
}

export interface MyBookingDetailResponse {
  bookingId: number;
  slotId: number;
  startAt: string;
  endAt: string;
  className: string;
  status: BookingStatus;
  depositAmount: number;
  balanceAmount: number;
  balanceStatus: string;
  passBooking: boolean;
}

export interface RescheduleRequest {
  newSlotId: number;
  token: string;
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
}
