import type {
  BookingCancelPolicyResponse,
  BookingDetailResponseStatus,
  MyBookingDetail,
} from "@/generated/api/booking";
import type { BookingPayload } from "@/generated/api/payment";

export type BookingStatus = BookingDetailResponseStatus;
export type DepositPaymentMethod = NonNullable<BookingPayload["paymentMethod"]>;
export type BookingCancelPolicy = BookingCancelPolicyResponse;
export type MyBookingDetailResponse = MyBookingDetail;
