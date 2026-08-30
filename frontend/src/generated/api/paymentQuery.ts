import { generatedApiClient } from '../../shared/api/generatedClient';
export interface PassPaymentPolicyResponse {
  totalCredits: number;
  totalPrice: number;
  validityDays: number;
}

export type PaymentStatusResponseContext = typeof PaymentStatusResponseContext[keyof typeof PaymentStatusResponseContext];


export const PaymentStatusResponseContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export type PaymentStatusResponseStatus = typeof PaymentStatusResponseStatus[keyof typeof PaymentStatusResponseStatus];


export const PaymentStatusResponseStatus = {
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

export interface PaymentStatusResponse {
  accessRecoveryRequired: boolean;
  /** @nullable */
  accessToken: string | null;
  amount: number;
  context: PaymentStatusResponseContext;
  /** @nullable */
  domainId: number | null;
  /** @nullable */
  receiptUrl: string | null;
  status: PaymentStatusResponseStatus;
}

export const getGetPassPaymentPolicyUrl = () => {




  return `/api/v1/payments/pass-policy`
}

export const getPassPaymentPolicy = async ( options?: RequestInit): Promise<PassPaymentPolicyResponse> => {

  return generatedApiClient<PassPaymentPolicyResponse>(getGetPassPaymentPolicyUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetPaymentStatusUrl = (orderId: string,) => {




  return `/api/v1/payments/${orderId}`
}

export const getPaymentStatus = async (orderId: string, options?: RequestInit): Promise<PaymentStatusResponse> => {

  return generatedApiClient<PaymentStatusResponse>(getGetPaymentStatusUrl(orderId),
  {
    ...options,
    method: 'GET'


  }
);}
