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
  pgRefundAmount: number;
  restoreCoupon: boolean;
  rewardRestoreAmount: number;
  rewardRevokeAmount: number;
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

/**
 * @nullable
 */
export type FulfillmentDtoCarrierCode = typeof FulfillmentDtoCarrierCode[keyof typeof FulfillmentDtoCarrierCode] | null;


export const FulfillmentDtoCarrierCode = {
  CJ_LOGISTICS: 'CJ_LOGISTICS',
  LOTTE: 'LOTTE',
  HANJIN: 'HANJIN',
  KOREA_POST: 'KOREA_POST',
  KYUNGDONG: 'KYUNGDONG',
  DAESIN: 'DAESIN',
  LOGEN: 'LOGEN',
  HAPDONG: 'HAPDONG',
  COUPANG: 'COUPANG',
  WOORI: 'WOORI',
  CU_POST: 'CU_POST',
  GS_POSTBOX: 'GS_POSTBOX',
} as const;

export interface ShippingAddressDto {
  addressLine1: string;
  /** @nullable */
  addressLine2: string | null;
  phone: string;
  postalCode: string;
  recipientName: string;
}

export type TrackingEventDtoStatus = typeof TrackingEventDtoStatus[keyof typeof TrackingEventDtoStatus];


export const TrackingEventDtoStatus = {
  PENDING: 'PENDING',
  REGISTERED: 'REGISTERED',
  PICKUP_READY: 'PICKUP_READY',
  PICKED_UP: 'PICKED_UP',
  IN_TRANSIT: 'IN_TRANSIT',
  OUT_FOR_DELIVERY: 'OUT_FOR_DELIVERY',
  DELIVERED: 'DELIVERED',
  FAILED: 'FAILED',
  RETURNED: 'RETURNED',
  CANCELLED: 'CANCELLED',
  HOLD: 'HOLD',
  UNKNOWN: 'UNKNOWN',
} as const;

export interface TrackingEventDto {
  /** @nullable */
  description: string | null;
  /** @nullable */
  location: string | null;
  occurredAt: string;
  status: TrackingEventDtoStatus;
  statusText: string;
}

/**
 * @nullable
 */
export type FulfillmentDtoTrackingRegistrationStatus = typeof FulfillmentDtoTrackingRegistrationStatus[keyof typeof FulfillmentDtoTrackingRegistrationStatus] | null;


export const FulfillmentDtoTrackingRegistrationStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  ACTIVE: 'ACTIVE',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
} as const;

/**
 * @nullable
 */
export type FulfillmentDtoTrackingStatus = typeof FulfillmentDtoTrackingStatus[keyof typeof FulfillmentDtoTrackingStatus] | null;


export const FulfillmentDtoTrackingStatus = {
  PENDING: 'PENDING',
  REGISTERED: 'REGISTERED',
  PICKUP_READY: 'PICKUP_READY',
  PICKED_UP: 'PICKED_UP',
  IN_TRANSIT: 'IN_TRANSIT',
  OUT_FOR_DELIVERY: 'OUT_FOR_DELIVERY',
  DELIVERED: 'DELIVERED',
  FAILED: 'FAILED',
  RETURNED: 'RETURNED',
  CANCELLED: 'CANCELLED',
  HOLD: 'HOLD',
  UNKNOWN: 'UNKNOWN',
} as const;

export type FulfillmentDtoType = typeof FulfillmentDtoType[keyof typeof FulfillmentDtoType];


export const FulfillmentDtoType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

export interface FulfillmentDto {
  /** @nullable */
  carrier: string | null;
  /** @nullable */
  carrierCode: FulfillmentDtoCarrierCode;
  /** @nullable */
  expectedShipDate: string | null;
  /** @nullable */
  pickupDeadlineAt: string | null;
  shippingAddress: ShippingAddressDto | null;
  trackingEvents: TrackingEventDto[];
  /** @nullable */
  trackingNumber: string | null;
  /** @nullable */
  trackingRegistrationStatus: FulfillmentDtoTrackingRegistrationStatus;
  /** @nullable */
  trackingStatus: FulfillmentDtoTrackingStatus;
  /** @nullable */
  trackingStatusText: string | null;
  /** @nullable */
  trackingUpdatedAt: string | null;
  type: FulfillmentDtoType;
  version: number;
}

export type OrderOptionSnapshotResponseType = typeof OrderOptionSnapshotResponseType[keyof typeof OrderOptionSnapshotResponseType];


export const OrderOptionSnapshotResponseType = {
  SELECT: 'SELECT',
  TEXT: 'TEXT',
} as const;

export interface OrderOptionSnapshotResponse {
  groupName: string;
  priceAdjustment: number;
  type: OrderOptionSnapshotResponseType;
  value: string;
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
  basePrice: number;
  /** @nullable */
  careInstructions: string | null;
  couponDiscountAmount: number;
  grossAmount: number;
  netPaidAmount: number;
  options: OrderOptionSnapshotResponse[];
  orderItemId: number;
  productId: number;
  productName: string;
  /** @nullable */
  productType: ItemDtoProductType;
  /** @nullable */
  productVariantId: number | null;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  rewardUsedAmount: number;
  /** @nullable */
  specification: string | null;
  textOptionPriceAdjustment: number;
  unitPrice: number;
  variantPriceAdjustment: number;
}

export interface OrderDetailResponse {
  /** @nullable */
  approvalDeadlineAt: string | null;
  couponDiscountAmount: number;
  fulfillment: FulfillmentDto | null;
  /** @nullable */
  issuedCouponId: number | null;
  items: ItemDto[];
  orderId: number;
  orderNumber: string;
  /** @nullable */
  paidAt: string | null;
  pgPaidAmount: number;
  productAmount: number;
  /** @nullable */
  receiptUrl: string | null;
  refund: RefundProgressResponse | null;
  rewardEarnBase: number;
  rewardUsedAmount: number;
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

export interface UpdateShippingAddressRequest {
  shippingAddress: ShippingAddress;
  version: number;
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



export const getUpdateGuestOrderShippingAddressUrl = (id: number,) => {




  return `/api/v1/orders/${id}/shipping-address`
}

export const updateGuestOrderShippingAddress = async (id: number,
    updateShippingAddressRequest: UpdateShippingAddressRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUpdateGuestOrderShippingAddressUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateShippingAddressRequest)
  }
);}
