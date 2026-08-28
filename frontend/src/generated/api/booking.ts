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
  pgRefundAmount: number;
  restoreCoupon: boolean;
  rewardRestoreAmount: number;
  rewardRevokeAmount: number;
  status: RefundProgressResponseStatus;
}

export interface CancelResponse {
  bookingId: number;
  manualCompensationRequired: boolean;
  /** @minimum 1 */
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
  /** @minimum 1 */
  participantCount: number;
  refund: RefundProgressResponse | null;
  slotId: number;
  startAt: string;
  status: BookingDetailResponseStatus;
}

export interface ReduceBookingParticipantsRequest {
  /** @minimum 1 */
  participantCount: number;
}

export type ReduceBookingParticipantsResponseStatus = typeof ReduceBookingParticipantsResponseStatus[keyof typeof ReduceBookingParticipantsResponseStatus];


export const ReduceBookingParticipantsResponseStatus = {
  BOOKED: 'BOOKED',
} as const;

export interface ReduceBookingParticipantsResponse {
  /** @minimum 0 */
  balanceAmount: number;
  bookingId: number;
  /** @minimum 1 */
  canceledParticipantCount: number;
  /** @minimum 0 */
  depositAmount: number;
  /** @minimum 1 */
  participantCount: number;
  refund: RefundProgressResponse | null;
  /** @minimum 0 */
  refundAmount: number;
  status: ReduceBookingParticipantsResponseStatus;
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
  /** @minimum 1 */
  participantCount: number;
  slotId: number;
  startAt: string;
  status: RescheduleResponseStatus;
}

export type ClassResponseStatus = typeof ClassResponseStatus[keyof typeof ClassResponseStatus];


export const ClassResponseStatus = {
  ACTIVE: 'ACTIVE',
  INACTIVE: 'INACTIVE',
} as const;

export interface ClassResponse {
  bufferMin: number;
  /** @minimum 1 */
  capacity: number;
  category: string;
  /** @nullable */
  description: string | null;
  durationMin: number;
  id: number;
  /** @nullable */
  imageUrl: string | null;
  name: string;
  passEligible: boolean;
  /** @nullable */
  preparationInfo: string | null;
  price: number;
  status: ClassResponseStatus;
  /** @nullable */
  targetAudience: string | null;
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
  /** @minimum 1 */
  participantCount: number;
  startAt: string;
  status: MyBookingSummaryStatus;
}

export interface MyBookingPageResponse {
  content: MyBookingSummary[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
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
  /** @minimum 1 */
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

export type VacancyAlertResponseStatus = typeof VacancyAlertResponseStatus[keyof typeof VacancyAlertResponseStatus];


export const VacancyAlertResponseStatus = {
  WAITING: 'WAITING',
  NOTIFIED: 'NOTIFIED',
  CANCELED: 'CANCELED',
} as const;

export interface VacancyAlertResponse {
  /** @nullable */
  accessToken: string | null;
  alertId: number;
  slotId: number;
  status: VacancyAlertResponseStatus;
}

export interface PublicSlotResponse {
  bookedCount: number;
  capacity: number;
  classId: number;
  endAt: string;
  id: number;
  remainingCapacity: number;
  startAt: string;
}

export interface GuestVacancyAlertRequest {
  /** @minLength 1 */
  name: string;
  /**
     * @minLength 1
     * @pattern ^01[0-9]{8,9}$
     */
  phone: string;
  /**
     * @minLength 1
     * @pattern ^[0-9]{6}$
     */
  verificationCode: string;
}

export type ListMyBookingsPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListAvailableSlotsParams = {
classId: number;
date: string;
};

export type ListUpcomingSlotsParams = {
classId: number;
/**
 * @minimum 1
 * @maximum 30
 */
days?: number;
includeFull?: boolean;
};

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



export const getReduceGuestBookingParticipantsUrl = (bookingId: number,) => {




  return `/api/v1/bookings/${bookingId}/participants`
}

export const reduceGuestBookingParticipants = async (bookingId: number,
    reduceBookingParticipantsRequest: ReduceBookingParticipantsRequest, options?: RequestInit): Promise<ReduceBookingParticipantsResponse> => {

  return generatedApiClient<ReduceBookingParticipantsResponse>(getReduceGuestBookingParticipantsUrl(bookingId),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(reduceBookingParticipantsRequest)
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



export const getListPublicClassesUrl = () => {




  return `/api/v1/classes`
}

export const listPublicClasses = async ( options?: RequestInit): Promise<ClassResponse[]> => {

  return generatedApiClient<ClassResponse[]>(getListPublicClassesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetPublicClassUrl = (id: number,) => {




  return `/api/v1/classes/${id}`
}

export const getPublicClass = async (id: number, options?: RequestInit): Promise<ClassResponse> => {

  return generatedApiClient<ClassResponse>(getGetPublicClassUrl(id),
  {
    ...options,
    method: 'GET'


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



export const getListMyBookingsPageUrl = (params?: ListMyBookingsPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/bookings/page?${stringifiedParams}` : `/api/v1/me/bookings/page`
}

export const listMyBookingsPage = async (params?: ListMyBookingsPageParams, options?: RequestInit): Promise<MyBookingPageResponse> => {

  return generatedApiClient<MyBookingPageResponse>(getListMyBookingsPageUrl(params),
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



export const getReduceMyBookingParticipantsUrl = (id: number,) => {




  return `/api/v1/me/bookings/${id}/participants`
}

export const reduceMyBookingParticipants = async (id: number,
    reduceBookingParticipantsRequest: ReduceBookingParticipantsRequest, options?: RequestInit): Promise<ReduceBookingParticipantsResponse> => {

  return generatedApiClient<ReduceBookingParticipantsResponse>(getReduceMyBookingParticipantsUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(reduceBookingParticipantsRequest)
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



export const getCancelMyVacancyAlertUrl = (slotId: number,) => {




  return `/api/v1/me/slots/${slotId}/vacancy-alerts`
}

export const cancelMyVacancyAlert = async (slotId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getCancelMyVacancyAlertUrl(slotId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getRegisterMyVacancyAlertUrl = (slotId: number,) => {




  return `/api/v1/me/slots/${slotId}/vacancy-alerts`
}

export const registerMyVacancyAlert = async (slotId: number, options?: RequestInit): Promise<VacancyAlertResponse> => {

  return generatedApiClient<VacancyAlertResponse>(getRegisterMyVacancyAlertUrl(slotId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getListAvailableSlotsUrl = (params: ListAvailableSlotsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/slots?${stringifiedParams}` : `/api/v1/slots`
}

export const listAvailableSlots = async (params: ListAvailableSlotsParams, options?: RequestInit): Promise<PublicSlotResponse[]> => {

  return generatedApiClient<PublicSlotResponse[]>(getListAvailableSlotsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListUpcomingSlotsUrl = (params: ListUpcomingSlotsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/slots/upcoming?${stringifiedParams}` : `/api/v1/slots/upcoming`
}

export const listUpcomingSlots = async (params: ListUpcomingSlotsParams, options?: RequestInit): Promise<PublicSlotResponse[]> => {

  return generatedApiClient<PublicSlotResponse[]>(getListUpcomingSlotsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCancelGuestVacancyAlertUrl = (slotId: number,) => {




  return `/api/v1/slots/${slotId}/vacancy-alerts`
}

export const cancelGuestVacancyAlert = async (slotId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getCancelGuestVacancyAlertUrl(slotId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getRegisterGuestVacancyAlertUrl = (slotId: number,) => {




  return `/api/v1/slots/${slotId}/vacancy-alerts`
}

export const registerGuestVacancyAlert = async (slotId: number,
    guestVacancyAlertRequest: GuestVacancyAlertRequest, options?: RequestInit): Promise<VacancyAlertResponse> => {

  return generatedApiClient<VacancyAlertResponse>(getRegisterGuestVacancyAlertUrl(slotId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(guestVacancyAlertRequest)
  }
);}
