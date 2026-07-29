import { generatedApiClient } from '../../shared/api/generatedClient';
export interface OrderPricePolicyResponse {
  madeToOrderConsentText: string;
  madeToOrderConsentVersion: string;
  shippingFee: number;
}

export type OrderCustomerActionResponseStatus = typeof OrderCustomerActionResponseStatus[keyof typeof OrderCustomerActionResponseStatus];


export const OrderCustomerActionResponseStatus = {
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

export interface OrderCustomerActionResponse {
  orderId: number;
  refund: RefundProgressResponse | null;
  status: OrderCustomerActionResponseStatus;
}

export type OrderDetailResponseStatus = typeof OrderDetailResponseStatus[keyof typeof OrderDetailResponseStatus];


export const OrderDetailResponseStatus = {
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

export interface ShippingAddressDto {
  addressLine1: string;
  /** @nullable */
  addressLine2: string | null;
  phone: string;
  postalCode: string;
  recipientName: string;
}

export type FulfillmentDtoType = typeof FulfillmentDtoType[keyof typeof FulfillmentDtoType];


export const FulfillmentDtoType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

export interface FulfillmentDto {
  /** @nullable */
  carrier: string | null;
  /** @nullable */
  expectedShipDate: string | null;
  /** @nullable */
  pickupDeadlineAt: string | null;
  shippingAddress: ShippingAddressDto | null;
  /** @nullable */
  trackingNumber: string | null;
  type: FulfillmentDtoType;
}

/**
 * @nullable
 */
export type ItemDtoProductType = typeof ItemDtoProductType[keyof typeof ItemDtoProductType] | null;


export const ItemDtoProductType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface ItemDto {
  /** @nullable */
  careInstructions: string | null;
  orderItemId: number;
  productId: number;
  productName: string;
  /** @nullable */
  productType: ItemDtoProductType;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  /** @nullable */
  specification: string | null;
  unitPrice: number;
}

export interface OrderDetailResponse {
  /** @nullable */
  approvalDeadlineAt: string | null;
  fulfillment: FulfillmentDto | null;
  items: ItemDto[];
  orderId: number;
  orderNumber: string;
  /** @nullable */
  paidAt: string | null;
  refund: RefundProgressResponse | null;
  shippingFee: number;
  status: OrderDetailResponseStatus;
  totalAmount: number;
}

export type OrderDelayResponseRequestDecision = typeof OrderDelayResponseRequestDecision[keyof typeof OrderDelayResponseRequestDecision];


export const OrderDelayResponseRequestDecision = {
  ACCEPT: 'ACCEPT',
  REJECT: 'REJECT',
} as const;

export interface OrderDelayResponseRequest {
  decision: OrderDelayResponseRequestDecision;
}

export const getGetOrderPricePolicyUrl = () => {




  return `/api/v1/orders/policy`
}

export const getOrderPricePolicy = async ( options?: RequestInit): Promise<OrderPricePolicyResponse> => {

  return generatedApiClient<OrderPricePolicyResponse>(getGetOrderPricePolicyUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCancelGuestOrderUrl = (id: number,) => {




  return `/api/v1/orders/${id}`
}

export const cancelGuestOrder = async (id: number, options?: RequestInit): Promise<OrderCustomerActionResponse> => {

  return generatedApiClient<OrderCustomerActionResponse>(getCancelGuestOrderUrl(id),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetGuestOrderUrl = (id: number,) => {




  return `/api/v1/orders/${id}`
}

export const getGuestOrder = async (id: number, options?: RequestInit): Promise<OrderDetailResponse> => {

  return generatedApiClient<OrderDetailResponse>(getGetGuestOrderUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRespondToGuestOrderDelayUrl = (id: number,) => {




  return `/api/v1/orders/${id}/delay-response`
}

export const respondToGuestOrderDelay = async (id: number,
    orderDelayResponseRequest: OrderDelayResponseRequest, options?: RequestInit): Promise<OrderCustomerActionResponse> => {

  return generatedApiClient<OrderCustomerActionResponse>(getRespondToGuestOrderDelayUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(orderDelayResponseRequest)
  }
);}
