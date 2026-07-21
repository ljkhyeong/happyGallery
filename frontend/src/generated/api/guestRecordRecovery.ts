import { generatedApiClient } from '../../shared/api/generatedClient';
export interface RecoverPaymentStatusesRequest {
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

export type PaymentSummaryContext = typeof PaymentSummaryContext[keyof typeof PaymentSummaryContext];


export const PaymentSummaryContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export type PaymentSummaryStatus = typeof PaymentSummaryStatus[keyof typeof PaymentSummaryStatus];


export const PaymentSummaryStatus = {
  READY: 'READY',
  CONFIRMING: 'CONFIRMING',
  RETRYABLE: 'RETRYABLE',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
  REVIEW_REQUIRED: 'REVIEW_REQUIRED',
  REFUNDING: 'REFUNDING',
  REFUNDED: 'REFUNDED',
  SUPPORT_REQUIRED: 'SUPPORT_REQUIRED',
  EXPIRED: 'EXPIRED',
} as const;

export interface PaymentSummary {
  amount: number;
  context: PaymentSummaryContext;
  orderId: string;
  status: PaymentSummaryStatus;
}

export interface PaymentStatusRecoveryResponse {
  expiresAt: string;
  payments: PaymentSummary[];
  statusToken: string;
}

export interface RecoverGuestRecordsRequest {
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

export interface BookingSummary {
  bookingId?: number;
  className?: string;
  endAt?: string;
  startAt?: string;
  status?: string;
}

export interface OrderSummary {
  createdAt?: string;
  orderId?: number;
  status?: string;
  totalAmount?: number;
}

export interface GuestRecordRecoveryResponse {
  accessToken?: string;
  bookings?: BookingSummary[];
  expiresAt?: string;
  orders?: OrderSummary[];
}

export const getRecoverGuestPaymentStatusesUrl = () => {




  return `/api/v1/guest-records/payment-status-recovery`
}

export const recoverGuestPaymentStatuses = async (recoverPaymentStatusesRequest: RecoverPaymentStatusesRequest, options?: RequestInit): Promise<PaymentStatusRecoveryResponse> => {

  return generatedApiClient<PaymentStatusRecoveryResponse>(getRecoverGuestPaymentStatusesUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(recoverPaymentStatusesRequest)
  }
);}



export const getRecoverUrl = () => {




  return `/api/v1/guest-records/recovery`
}

export const recover = async (recoverGuestRecordsRequest: RecoverGuestRecordsRequest, options?: RequestInit): Promise<GuestRecordRecoveryResponse> => {

  return generatedApiClient<GuestRecordRecoveryResponse>(getRecoverUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(recoverGuestRecordsRequest)
  }
);}
