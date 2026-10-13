import type {
  BookingCancelPolicyResponse,
  BookingDetailResponse as GeneratedBookingDetailResponse,
  BookingDetailResponseStatus,
  CancelResponse as GeneratedCancelResponse,
  MyBookingDetail,
} from "@/generated/api/booking";
import type { BookingPayload } from "@/generated/api/payment";

export type BookingStatus = BookingDetailResponseStatus;
export type DepositPaymentMethod = NonNullable<BookingPayload["paymentMethod"]>;
export type BookingCancelPolicy = BookingCancelPolicyResponse;
export type BookingDetailResponse = GeneratedBookingDetailResponse;
export type MyBookingDetailResponse = MyBookingDetail;
export type CancelResponse = GeneratedCancelResponse;
