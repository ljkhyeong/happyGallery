import { generatedApiClient } from '../../shared/api/generatedClient';
export interface ConfirmPaymentRequest {
  amount?: number;
  /** @minLength 1 */
  orderId: string;
  paymentKey?: string;
}

export type ConfirmPaymentResponseContext = typeof ConfirmPaymentResponseContext[keyof typeof ConfirmPaymentResponseContext];


export const ConfirmPaymentResponseContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export interface ConfirmPaymentResponse {
  accessRecoveryRequired: boolean;
  /** @nullable */
  accessToken: string | null;
  context: ConfirmPaymentResponseContext;
  /** @nullable */
  domainId: number | null;
}

export type PreparePaymentRequestContext = typeof PreparePaymentRequestContext[keyof typeof PreparePaymentRequestContext];


export const PreparePaymentRequestContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export interface PaymentPayload {
  type: string;
}

export type BookingPayloadPaymentMethod = typeof BookingPayloadPaymentMethod[keyof typeof BookingPayloadPaymentMethod];


export const BookingPayloadPaymentMethod = {
  CARD: 'CARD',
  EASY_PAY: 'EASY_PAY',
  BANK_TRANSFER: 'BANK_TRANSFER',
} as const;

export interface PolicyAcceptance {
  privacyAccepted?: boolean;
  privacyVersion?: string;
  termsAccepted?: boolean;
  termsVersion?: string;
}

export type BookingPayload = PaymentPayload & {
  name?: string;
  /**
     * @minimum 1
     * @maximum 8
     */
  participantCount: number;
  passId?: number;
  paymentMethod?: BookingPayloadPaymentMethod;
  phone?: string;
  policyAcceptance?: PolicyAcceptance;
  slotId?: number;
  userId?: number;
  verificationCode?: string;
};

export type OrderPayloadFulfillmentType = typeof OrderPayloadFulfillmentType[keyof typeof OrderPayloadFulfillmentType];


export const OrderPayloadFulfillmentType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

export interface OrderItemRef {
  productId?: number;
  qty?: number;
}

export interface ShippingAddress {
  addressLine1?: string;
  addressLine2?: string;
  phone?: string;
  postalCode?: string;
  recipientName?: string;
}

export type OrderPayload = PaymentPayload & {
  cartCheckout: boolean;
  fulfillmentType?: OrderPayloadFulfillmentType;
  items?: OrderItemRef[];
  madeToOrderConsent: boolean;
  madeToOrderConsentVersion?: string;
  name?: string;
  phone?: string;
  policyAcceptance?: PolicyAcceptance;
  shippingAddress?: ShippingAddress;
  userId?: number;
  verificationCode?: string;
};

export type PassPayload = PaymentPayload & {
  userId?: number;
};

export interface PreparePaymentRequest {
  context: PreparePaymentRequestContext;
  payload: BookingPayload | OrderPayload | PassPayload;
}

export type PreparePaymentResponseContext = typeof PreparePaymentResponseContext[keyof typeof PreparePaymentResponseContext];


export const PreparePaymentResponseContext = {
  ORDER: 'ORDER',
  BOOKING: 'BOOKING',
  PASS: 'PASS',
} as const;

export interface PreparePaymentResponse {
  amount: number;
  context: PreparePaymentResponseContext;
  orderId: string;
  /** @nullable */
  statusToken: string | null;
}

export const getConfirmPaymentUrl = () => {




  return `/api/v1/payments/confirm`
}

export const confirmPayment = async (confirmPaymentRequest: ConfirmPaymentRequest, options?: RequestInit): Promise<ConfirmPaymentResponse> => {

  return generatedApiClient<ConfirmPaymentResponse>(getConfirmPaymentUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(confirmPaymentRequest)
  }
);}



export const getPreparePaymentUrl = () => {




  return `/api/v1/payments/prepare`
}

export const preparePayment = async (preparePaymentRequest: PreparePaymentRequest, options?: RequestInit): Promise<PreparePaymentResponse> => {

  return generatedApiClient<PreparePaymentResponse>(getPreparePaymentUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(preparePaymentRequest)
  }
);}
