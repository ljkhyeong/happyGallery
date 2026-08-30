import { generatedApiClient } from '../../shared/api/generatedClient';
export interface ConfirmPaymentRequest {
  amount: number;
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
  /** @nullable */
  receiptUrl: string | null;
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

export type OrderPayloadType = typeof OrderPayloadType[keyof typeof OrderPayloadType];


export const OrderPayloadType = {
  ORDER: 'ORDER',
} as const;

export type OrderPayloadFulfillmentType = typeof OrderPayloadFulfillmentType[keyof typeof OrderPayloadFulfillmentType];


export const OrderPayloadFulfillmentType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

export interface OrderTextInput {
  /**
     * @minLength 1
     * @pattern ^[A-Za-z0-9_-]{1,64}$
     */
  groupKey: string;
  /**
     * @minLength 0
     * @maxLength 200
     */
  value?: string;
}

export interface OrderItemRef {
  productId: number;
  /** @nullable */
  productVariantId?: number | null;
  /**
     * @minimum 1
     * @maximum 99
     */
  qty: number;
  /**
     * @minItems 0
     * @maxItems 5
     */
  textInputs?: OrderTextInput[];
}

export interface PolicyAcceptanceRequest {
  privacyAccepted: boolean;
  /** @minLength 1 */
  privacyVersion: string;
  termsAccepted: boolean;
  /** @minLength 1 */
  termsVersion: string;
}

export interface ShippingAddress {
  /** @minLength 1 */
  addressLine1: string;
  /** @nullable */
  addressLine2?: string | null;
  /** @minLength 1 */
  phone: string;
  /**
     * @minLength 1
     * @pattern ^[0-9]{5}$
     */
  postalCode: string;
  /** @minLength 1 */
  recipientName: string;
}

export type OrderPayload = Omit<PaymentPayload, 'type'> & ({
  type: OrderPayloadType;
  cartCheckout: boolean;
  /**
     * GET /api/v1/me/cart가 반환한 불투명 장바구니 스냅샷 버전
     * @nullable
     * @pattern ^[0-9a-f]{64}$
     */
  expectedCartVersion?: string | null;
  fulfillmentType: OrderPayloadFulfillmentType;
  /** @nullable */
  issuedCouponId?: number | null;
  /**
     * @minItems 0
     * @maxItems 100
     */
  items: OrderItemRef[];
  madeToOrderConsent: boolean;
  /** @nullable */
  madeToOrderConsentVersion?: string | null;
  /** @nullable */
  name?: string | null;
  /** @nullable */
  phone?: string | null;
  policyAcceptance?: PolicyAcceptanceRequest | null;
  /**
     * @minimum 0
     * @maximum 9007199254740991
     * @nullable
     */
  rewardAmount?: number | null;
  shippingAddress?: ShippingAddress | null;
  /** @nullable */
  userId?: number | null;
  /** @nullable */
  verificationCode?: string | null;
});

export type BookingPayloadType = typeof BookingPayloadType[keyof typeof BookingPayloadType];


export const BookingPayloadType = {
  BOOKING: 'BOOKING',
} as const;

/**
 * @nullable
 */
export type BookingPayloadPaymentMethod = typeof BookingPayloadPaymentMethod[keyof typeof BookingPayloadPaymentMethod] | null;


export const BookingPayloadPaymentMethod = {
  CARD: 'CARD',
  EASY_PAY: 'EASY_PAY',
} as const;

export type BookingPayload = Omit<PaymentPayload, 'type'> & ({
  type: BookingPayloadType;
  /** @nullable */
  name?: string | null;
  /** @minimum 1 */
  participantCount: number;
  /** @nullable */
  passId?: number | null;
  /** @nullable */
  paymentMethod?: BookingPayloadPaymentMethod;
  /** @nullable */
  phone?: string | null;
  policyAcceptance?: PolicyAcceptanceRequest | null;
  slotId: number;
  /** @nullable */
  userId?: number | null;
  /** @nullable */
  verificationCode?: string | null;
});

export type PassPayloadType = typeof PassPayloadType[keyof typeof PassPayloadType];


export const PassPayloadType = {
  PASS: 'PASS',
} as const;

export type PassPayload = Omit<PaymentPayload, 'type'> & {
  type: PassPayloadType;
  userId: number;
};

export interface PreparePaymentRequest {
  context: PreparePaymentRequestContext;
  payload: OrderPayload | BookingPayload | PassPayload;
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
