import type {
  BookingCancelPolicyResponse,
  BookingDetailResponse as GeneratedBookingDetailResponse,
  BookingDetailResponseStatus,
  CancelResponse as GeneratedCancelResponse,
  MyBookingDetail,
  RescheduleResponse as GeneratedRescheduleResponse,
  SendVerificationRequest as GeneratedSendVerificationRequest,
  SendVerificationResponse as GeneratedSendVerificationResponse,
} from "@/generated/api/booking";
import type { BookingPayload } from "@/generated/api/payment";

export type BookingStatus = BookingDetailResponseStatus;
export type DepositPaymentMethod = NonNullable<BookingPayload["paymentMethod"]>;
export type BookingCancelPolicy = BookingCancelPolicyResponse;
export type SendVerificationRequest = GeneratedSendVerificationRequest;
export type SendVerificationResponse = GeneratedSendVerificationResponse;
export type BookingDetailResponse = GeneratedBookingDetailResponse;
export type MyBookingDetailResponse = MyBookingDetail;
export type RescheduleResponse = GeneratedRescheduleResponse;
export type CancelResponse = GeneratedCancelResponse;
