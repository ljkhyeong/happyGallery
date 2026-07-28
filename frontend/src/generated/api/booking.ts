import { generatedApiClient } from '../../shared/api/generatedClient';
export type SendVerificationRequestPurpose = typeof SendVerificationRequestPurpose[keyof typeof SendVerificationRequestPurpose];


export const SendVerificationRequestPurpose = {
  SIGNUP: 'SIGNUP',
  PASSWORD_RESET: 'PASSWORD_RESET',
  MEMBER_PHONE_REGISTRATION: 'MEMBER_PHONE_REGISTRATION',
  MEMBER_PHONE_CHANGE: 'MEMBER_PHONE_CHANGE',
  GUEST_BOOKING: 'GUEST_BOOKING',
  GUEST_ORDER: 'GUEST_ORDER',
  GUEST_CLAIM: 'GUEST_CLAIM',
  GUEST_RECORD_RECOVERY: 'GUEST_RECORD_RECOVERY',
  GUEST_PAYMENT_STATUS_RECOVERY: 'GUEST_PAYMENT_STATUS_RECOVERY',
} as const;

export interface SendVerificationRequest {
  /**
     * @minLength 1
     * @pattern ^01[0-9]{8,9}$
     */
  phone: string;
  purpose: SendVerificationRequestPurpose;
}

export interface SendVerificationResponse {
  phone: string;
  verificationId: number;
}

export type CancelResponseStatus = typeof CancelResponseStatus[keyof typeof CancelResponseStatus];


export const CancelResponseStatus = {
  CANCELED: 'CANCELED',
} as const;

export type RefundProgressResponseStatus = typeof RefundProgressResponseStatus[keyof typeof RefundProgressResponseStatus];


export const RefundProgressResponseStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export interface RefundProgressResponse {
  amount: number;
  status: RefundProgressResponseStatus;
}

export interface CancelResponse {
  bookingId: number;
  manualCompensationRequired: boolean;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  refund: RefundProgressResponse | null;
  refundAmount: number;
  refundable: boolean;
  status: CancelResponseStatus;
}

export type BookingDetailResponseStatus = typeof BookingDetailResponseStatus[keyof typeof BookingDetailResponseStatus];


export const BookingDetailResponseStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface BookingCancelPolicyResponse {
  cancellable: boolean;
  deadlineAt: string;
  manualCompensationRequired: boolean;
  passCreditRestorable: boolean;
  refundable: boolean;
  /** @nullable */
  warningCode: string | null;
}

export interface BookingDetailResponse {
  balanceAmount: number;
  bookingId: number;
  bookingNumber: string;
  cancelPolicy: BookingCancelPolicyResponse;
  classId: number;
  className: string;
  depositAmount: number;
  endAt: string;
  guestName: string;
  guestPhone: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  refund: RefundProgressResponse | null;
  slotId: number;
  startAt: string;
  status: BookingDetailResponseStatus;
}

export interface RescheduleRequest {
  newSlotId: number;
}

export type RescheduleResponseStatus = typeof RescheduleResponseStatus[keyof typeof RescheduleResponseStatus];


export const RescheduleResponseStatus = {
  BOOKED: 'BOOKED',
} as const;

export interface RescheduleResponse {
  bookingId: number;
  bookingNumber: string;
  className: string;
  endAt: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  slotId: number;
  startAt: string;
  status: RescheduleResponseStatus;
}

export type MyBookingSummaryStatus = typeof MyBookingSummaryStatus[keyof typeof MyBookingSummaryStatus];


export const MyBookingSummaryStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface MyBookingSummary {
  bookingId: number;
  className: string;
  depositAmount: number;
  endAt: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  startAt: string;
  status: MyBookingSummaryStatus;
}

export type MyBookingDetailBalanceStatus = typeof MyBookingDetailBalanceStatus[keyof typeof MyBookingDetailBalanceStatus];


export const MyBookingDetailBalanceStatus = {
  UNPAID: 'UNPAID',
  PAID: 'PAID',
} as const;

export type MyBookingDetailStatus = typeof MyBookingDetailStatus[keyof typeof MyBookingDetailStatus];


export const MyBookingDetailStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface MyBookingDetail {
  balanceAmount: number;
  balanceStatus: MyBookingDetailBalanceStatus;
  bookingId: number;
  cancelPolicy: BookingCancelPolicyResponse;
  classId: number;
  className: string;
  depositAmount: number;
  endAt: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  passBooking: boolean;
  refund: RefundProgressResponse | null;
  slotId: number;
  startAt: string;
  status: MyBookingDetailStatus;
}

export interface MemberRescheduleRequest {
  newSlotId: number;
}

export const getSendGuestBookingVerificationUrl = () => {




  return `/api/v1/bookings/phone-verifications`
}

export const sendGuestBookingVerification = async (sendVerificationRequest: SendVerificationRequest, options?: RequestInit): Promise<SendVerificationResponse> => {

  return generatedApiClient<SendVerificationResponse>(getSendGuestBookingVerificationUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(sendVerificationRequest)
  }
);}



export const getCancelGuestBookingUrl = (bookingId: number,) => {




  return `/api/v1/bookings/${bookingId}`
}

export const cancelGuestBooking = async (bookingId: number, options?: RequestInit): Promise<CancelResponse> => {

  return generatedApiClient<CancelResponse>(getCancelGuestBookingUrl(bookingId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetGuestBookingUrl = (bookingId: number,) => {




  return `/api/v1/bookings/${bookingId}`
}

export const getGuestBooking = async (bookingId: number, options?: RequestInit): Promise<BookingDetailResponse> => {

  return generatedApiClient<BookingDetailResponse>(getGetGuestBookingUrl(bookingId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRescheduleGuestBookingUrl = (bookingId: number,) => {




  return `/api/v1/bookings/${bookingId}/reschedule`
}

export const rescheduleGuestBooking = async (bookingId: number,
    rescheduleRequest: RescheduleRequest, options?: RequestInit): Promise<RescheduleResponse> => {

  return generatedApiClient<RescheduleResponse>(getRescheduleGuestBookingUrl(bookingId),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(rescheduleRequest)
  }
);}



export const getListMyBookingsUrl = () => {




  return `/api/v1/me/bookings`
}

export const listMyBookings = async ( options?: RequestInit): Promise<MyBookingSummary[]> => {

  return generatedApiClient<MyBookingSummary[]>(getListMyBookingsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCancelMyBookingUrl = (id: number,) => {




  return `/api/v1/me/bookings/${id}`
}

export const cancelMyBooking = async (id: number, options?: RequestInit): Promise<CancelResponse> => {

  return generatedApiClient<CancelResponse>(getCancelMyBookingUrl(id),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetMyBookingUrl = (id: number,) => {




  return `/api/v1/me/bookings/${id}`
}

export const getMyBooking = async (id: number, options?: RequestInit): Promise<MyBookingDetail> => {

  return generatedApiClient<MyBookingDetail>(getGetMyBookingUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRescheduleMyBookingUrl = (id: number,) => {




  return `/api/v1/me/bookings/${id}/reschedule`
}

export const rescheduleMyBooking = async (id: number,
    memberRescheduleRequest: MemberRescheduleRequest, options?: RequestInit): Promise<MyBookingSummary> => {

  return generatedApiClient<MyBookingSummary>(getRescheduleMyBookingUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(memberRescheduleRequest)
  }
);}
