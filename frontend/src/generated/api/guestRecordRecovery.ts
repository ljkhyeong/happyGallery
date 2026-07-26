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

export type GuestRecordRecoveryBookingSummaryStatus = typeof GuestRecordRecoveryBookingSummaryStatus[keyof typeof GuestRecordRecoveryBookingSummaryStatus];


export const GuestRecordRecoveryBookingSummaryStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface GuestRecordRecoveryBookingSummary {
  bookingId: number;
  className: string;
  endAt: string;
  startAt: string;
  status: GuestRecordRecoveryBookingSummaryStatus;
}

export type GuestRecordRecoveryOrderSummaryStatus = typeof GuestRecordRecoveryOrderSummaryStatus[keyof typeof GuestRecordRecoveryOrderSummaryStatus];


export const GuestRecordRecoveryOrderSummaryStatus = {
  PAID_APPROVAL_PENDING: 'PAID_APPROVAL_PENDING',
  APPROVED_FULFILLMENT_PENDING: 'APPROVED_FULFILLMENT_PENDING',
  REJECTED: 'REJECTED',
  CUSTOMER_CANCELED: 'CUSTOMER_CANCELED',
  AUTO_REFUND_TIMEOUT: 'AUTO_REFUND_TIMEOUT',
  IN_PRODUCTION: 'IN_PRODUCTION',
  DELAY_CONSENT_PENDING: 'DELAY_CONSENT_PENDING',
  DELAY_ACCEPTED: 'DELAY_ACCEPTED',
  DELAY_REJECTED_CANCELED: 'DELAY_REJECTED_CANCELED',
  SHIPPING_PREPARING: 'SHIPPING_PREPARING',
  SHIPPED: 'SHIPPED',
  DELIVERED: 'DELIVERED',
  PICKUP_READY: 'PICKUP_READY',
  PICKED_UP: 'PICKED_UP',
  PICKUP_EXPIRED: 'PICKUP_EXPIRED',
  PICKUP_FORFEITED: 'PICKUP_FORFEITED',
  COMPLETED: 'COMPLETED',
} as const;

export interface GuestRecordRecoveryOrderSummary {
  createdAt: string;
  orderId: number;
  status: GuestRecordRecoveryOrderSummaryStatus;
  totalAmount: number;
}

export interface GuestRecordRecoveryResponse {
  accessToken: string;
  bookings: GuestRecordRecoveryBookingSummary[];
  expiresAt: string;
  orders: GuestRecordRecoveryOrderSummary[];
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



export const getRecoverGuestRecordsUrl = () => {




  return `/api/v1/guest-records/recovery`
}

export const recoverGuestRecords = async (recoverGuestRecordsRequest: RecoverGuestRecordsRequest, options?: RequestInit): Promise<GuestRecordRecoveryResponse> => {

  return generatedApiClient<GuestRecordRecoveryResponse>(getRecoverGuestRecordsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(recoverGuestRecordsRequest)
  }
);}
