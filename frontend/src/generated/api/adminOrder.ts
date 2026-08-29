import { generatedApiClient } from '../../shared/api/generatedClient';
/**
 * @nullable
 */
export type AdminOrderListItemResponseFulfillmentType = typeof AdminOrderListItemResponseFulfillmentType[keyof typeof AdminOrderListItemResponseFulfillmentType] | null;


export const AdminOrderListItemResponseFulfillmentType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

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
export type AdminOrderItemResponseProductType = typeof AdminOrderItemResponseProductType[keyof typeof AdminOrderItemResponseProductType] | null;


export const AdminOrderItemResponseProductType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface AdminOrderItemResponse {
  basePrice: number;
  /** @nullable */
  careInstructions: string | null;
  options: OrderOptionSnapshotResponse[];
  productId: number;
  productName: string;
  /** @nullable */
  productType: AdminOrderItemResponseProductType;
  /** @nullable */
  productVariantId: number | null;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  /** @nullable */
  specification: string | null;
  textOptionPriceAdjustment: number;
  unitPrice: number;
  variantPriceAdjustment: number;
}

export type AdminOrderListItemResponseStatus = typeof AdminOrderListItemResponseStatus[keyof typeof AdminOrderListItemResponseStatus];


export const AdminOrderListItemResponseStatus = {
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

export interface AdminOrderListItemResponse {
  /** @nullable */
  approvalDeadlineAt: string | null;
  createdAt: string;
  /** @nullable */
  fulfillmentType: AdminOrderListItemResponseFulfillmentType;
  items: AdminOrderItemResponse[];
  orderId: number;
  orderNumber: string;
  /** @nullable */
  paidAt: string | null;
  shippingFee: number;
  status: AdminOrderListItemResponseStatus;
  totalAmount: number;
}

export interface AdminOrderPageResponse {
  content: AdminOrderListItemResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type BatchResponseFailureReasons = {[key: string]: number};

export interface BatchResponse {
  failureCount: number;
  failureReasons: BatchResponseFailureReasons;
  successCount: number;
}

export type AdminOrderSearchResultStatus = typeof AdminOrderSearchResultStatus[keyof typeof AdminOrderSearchResultStatus];


export const AdminOrderSearchResultStatus = {
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

export interface AdminOrderSearchResult {
  /** @nullable */
  approvalDeadlineAt: string | null;
  buyerName: string;
  /** @nullable */
  buyerPhone: string | null;
  createdAt: string;
  orderId: number;
  orderNumber: string;
  /** @nullable */
  paidAt: string | null;
  status: AdminOrderSearchResultStatus;
  totalAmount: number;
}

export interface AdminOrderSearchPageResponse {
  content: AdminOrderSearchResult[];
  page: number;
  size: number;
  totalCount: number;
  totalPages: number;
}

export type OrderDelayCancellationResponseOrderStatus = typeof OrderDelayCancellationResponseOrderStatus[keyof typeof OrderDelayCancellationResponseOrderStatus];


export const OrderDelayCancellationResponseOrderStatus = {
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

export type RefundStatusResponseStatus = typeof RefundStatusResponseStatus[keyof typeof RefundStatusResponseStatus];


export const RefundStatusResponseStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export interface RefundStatusResponse {
  amount: number;
  attemptCount: number;
  /** @nullable */
  failReason: string | null;
  pgRefundAmount: number;
  refundId: number;
  restoreCoupon: boolean;
  rewardRestoreAmount: number;
  rewardRevokeAmount: number;
  status: RefundStatusResponseStatus;
}

export interface OrderDelayCancellationResponse {
  /** @nullable */
  expectedShipDate: string | null;
  orderId: number;
  orderStatus: OrderDelayCancellationResponseOrderStatus;
  refund: RefundStatusResponse;
}

export type PickupResponseStatus = typeof PickupResponseStatus[keyof typeof PickupResponseStatus];


export const PickupResponseStatus = {
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

export interface PickupResponse {
  orderId: number;
  /** @nullable */
  pickupDeadlineAt: string | null;
  status: PickupResponseStatus;
}

export type OrderProductionResponseStatus = typeof OrderProductionResponseStatus[keyof typeof OrderProductionResponseStatus];


export const OrderProductionResponseStatus = {
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

export interface OrderProductionResponse {
  /** @nullable */
  expectedShipDate: string | null;
  orderId: number;
  status: OrderProductionResponseStatus;
}

export interface SetExpectedShipDateRequest {
  /** @nullable */
  expectedShipDate?: string | null;
}

/**
 * @nullable
 */
export type AdminOrderFulfillmentResponseCarrierCode = typeof AdminOrderFulfillmentResponseCarrierCode[keyof typeof AdminOrderFulfillmentResponseCarrierCode] | null;


export const AdminOrderFulfillmentResponseCarrierCode = {
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

/**
 * @nullable
 */
export type AdminOrderFulfillmentResponseTrackingRegistrationStatus = typeof AdminOrderFulfillmentResponseTrackingRegistrationStatus[keyof typeof AdminOrderFulfillmentResponseTrackingRegistrationStatus] | null;


export const AdminOrderFulfillmentResponseTrackingRegistrationStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  ACTIVE: 'ACTIVE',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
} as const;

/**
 * @nullable
 */
export type AdminOrderFulfillmentResponseTrackingStatus = typeof AdminOrderFulfillmentResponseTrackingStatus[keyof typeof AdminOrderFulfillmentResponseTrackingStatus] | null;


export const AdminOrderFulfillmentResponseTrackingStatus = {
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

export type AdminOrderFulfillmentResponseType = typeof AdminOrderFulfillmentResponseType[keyof typeof AdminOrderFulfillmentResponseType];


export const AdminOrderFulfillmentResponseType = {
  SHIPPING: 'SHIPPING',
  PICKUP: 'PICKUP',
} as const;

export interface AdminOrderShippingAddress {
  addressLine1: string;
  /** @nullable */
  addressLine2: string | null;
  phone: string;
  postalCode: string;
  recipientName: string;
}

export type TrackingEventStatus = typeof TrackingEventStatus[keyof typeof TrackingEventStatus];


export const TrackingEventStatus = {
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

export interface TrackingEvent {
  /** @nullable */
  description: string | null;
  /** @nullable */
  location: string | null;
  occurredAt: string;
  status: TrackingEventStatus;
  statusText: string;
}

export interface AdminOrderFulfillmentResponse {
  /** @nullable */
  carrier: string | null;
  /** @nullable */
  carrierCode: AdminOrderFulfillmentResponseCarrierCode;
  /** @nullable */
  expectedShipDate: string | null;
  orderId: number;
  /** @nullable */
  pickupDeadlineAt: string | null;
  shippingAddress: AdminOrderShippingAddress | null;
  trackingEvents: TrackingEvent[];
  /** @nullable */
  trackingNumber: string | null;
  /** @nullable */
  trackingRegistrationStatus: AdminOrderFulfillmentResponseTrackingRegistrationStatus;
  /** @nullable */
  trackingStatus: AdminOrderFulfillmentResponseTrackingStatus;
  /** @nullable */
  trackingStatusText: string | null;
  /** @nullable */
  trackingUpdatedAt: string | null;
  type: AdminOrderFulfillmentResponseType;
}

export type AdminOrderHistoryResponseDecision = typeof AdminOrderHistoryResponseDecision[keyof typeof AdminOrderHistoryResponseDecision];


export const AdminOrderHistoryResponseDecision = {
  APPROVE: 'APPROVE',
  REJECT: 'REJECT',
  CUSTOMER_CANCEL: 'CUSTOMER_CANCEL',
  DELAY: 'DELAY',
  DELAY_ACCEPT: 'DELAY_ACCEPT',
  DELAY_REJECT: 'DELAY_REJECT',
  DELAY_CANCEL: 'DELAY_CANCEL',
  AUTO_REFUND: 'AUTO_REFUND',
  SHIP_DATE_UPDATED: 'SHIP_DATE_UPDATED',
  PRODUCTION_COMPLETE: 'PRODUCTION_COMPLETE',
  RESUME_PRODUCTION: 'RESUME_PRODUCTION',
  PICKUP_READY: 'PICKUP_READY',
  PICKUP_COMPLETE: 'PICKUP_COMPLETE',
  PICKUP_EXPIRED: 'PICKUP_EXPIRED',
  PICKUP_FORFEITED: 'PICKUP_FORFEITED',
  PREPARE_SHIPPING: 'PREPARE_SHIPPING',
  SHIP: 'SHIP',
  DELIVER: 'DELIVER',
} as const;

export interface AdminOrderHistoryResponse {
  decidedAt: string;
  /** @nullable */
  decidedByAdminId: number | null;
  decision: AdminOrderHistoryResponseDecision;
  id: number;
  /** @nullable */
  reason: string | null;
}

/**
 * @nullable
 */
export type ShippingResponseCarrierCode = typeof ShippingResponseCarrierCode[keyof typeof ShippingResponseCarrierCode] | null;


export const ShippingResponseCarrierCode = {
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

export type ShippingResponseStatus = typeof ShippingResponseStatus[keyof typeof ShippingResponseStatus];


export const ShippingResponseStatus = {
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
export type ShippingResponseTrackingRegistrationStatus = typeof ShippingResponseTrackingRegistrationStatus[keyof typeof ShippingResponseTrackingRegistrationStatus] | null;


export const ShippingResponseTrackingRegistrationStatus = {
  PENDING: 'PENDING',
  PROCESSING: 'PROCESSING',
  ACTIVE: 'ACTIVE',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED',
} as const;

/**
 * @nullable
 */
export type ShippingResponseTrackingStatus = typeof ShippingResponseTrackingStatus[keyof typeof ShippingResponseTrackingStatus] | null;


export const ShippingResponseTrackingStatus = {
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

export interface ShippingResponse {
  /** @nullable */
  carrier: string | null;
  /** @nullable */
  carrierCode: ShippingResponseCarrierCode;
  /** @nullable */
  expectedShipDate: string | null;
  orderId: number;
  status: ShippingResponseStatus;
  /** @nullable */
  trackingNumber: string | null;
  /** @nullable */
  trackingRegistrationStatus: ShippingResponseTrackingRegistrationStatus;
  /** @nullable */
  trackingStatus: ShippingResponseTrackingStatus;
  /** @nullable */
  trackingStatusText: string | null;
  /** @nullable */
  trackingUpdatedAt: string | null;
}

/**
 * @nullable
 */
export type MarkShippedRequestCarrierCode = typeof MarkShippedRequestCarrierCode[keyof typeof MarkShippedRequestCarrierCode] | null;


export const MarkShippedRequestCarrierCode = {
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

export interface MarkShippedRequest {
  /**
     * @minLength 0
     * @maxLength 50
     */
  carrier: string;
  /** @nullable */
  carrierCode?: MarkShippedRequestCarrierCode;
  /**
     * @minLength 0
     * @maxLength 100
     */
  trackingNumber: string;
}

export interface MarkPickupReadyRequest {
  /** @nullable */
  pickupDeadlineAt?: string | null;
}

export type MissedPickupRefundResponseOrderStatus = typeof MissedPickupRefundResponseOrderStatus[keyof typeof MissedPickupRefundResponseOrderStatus];


export const MissedPickupRefundResponseOrderStatus = {
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

export interface MissedPickupRefundResponse {
  orderId: number;
  orderStatus: MissedPickupRefundResponseOrderStatus;
  refund: RefundStatusResponse;
}

export type OrderRejectResponseOrderStatus = typeof OrderRejectResponseOrderStatus[keyof typeof OrderRejectResponseOrderStatus];


export const OrderRejectResponseOrderStatus = {
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

export interface OrderRejectResponse {
  orderId: number;
  orderStatus: OrderRejectResponseOrderStatus;
  refund: RefundStatusResponse;
}

/**
 * @nullable
 */
export type SmartStoreChannelOrderResponseAttentionReason = typeof SmartStoreChannelOrderResponseAttentionReason[keyof typeof SmartStoreChannelOrderResponseAttentionReason] | null;


export const SmartStoreChannelOrderResponseAttentionReason = {
  MAPPING_REQUIRED: 'MAPPING_REQUIRED',
  STOCK_SHORTAGE: 'STOCK_SHORTAGE',
  RETURN_REVIEW: 'RETURN_REVIEW',
  STATUS_REVIEW: 'STATUS_REVIEW',
} as const;

export interface SmartStoreChannelOrderResponse {
  /** @nullable */
  attentionReason: SmartStoreChannelOrderResponseAttentionReason;
  /** @nullable */
  claimStatus: string | null;
  /** @nullable */
  claimType: string | null;
  initialQuantity: number;
  inventoryAppliedQuantity: number;
  /** @nullable */
  itemNo: number | null;
  lastChangedAt: string;
  orderId: string;
  originProductNo: number;
  /** @nullable */
  paymentDate: string | null;
  /** @nullable */
  productId: number | null;
  productName: string;
  /** @nullable */
  productOption: string | null;
  productOrderId: string;
  productOrderStatus: string;
  /** @nullable */
  productVariantId: number | null;
  remainQuantity: number;
}

export interface ClaimDetail {
  /** @nullable */
  claimDeliveryFeeDemandAmount: number | null;
  /** @nullable */
  claimId: string | null;
  /** @nullable */
  claimStatus: string | null;
  claimType: string;
  /** @nullable */
  collectDeliveryCompany: string | null;
  /** @nullable */
  collectStatus: string | null;
  /** @nullable */
  collectTrackingNumber: string | null;
  /** @nullable */
  detailedReason: string | null;
  /** @nullable */
  holdbackStatus: string | null;
  imageUrls: string[];
  /** @nullable */
  reason: string | null;
  /** @nullable */
  requestQuantity: number | null;
  /** @nullable */
  requestedAt: string | null;
}

export interface DeliveryInfo {
  /** @nullable */
  addressLine1: string | null;
  /** @nullable */
  addressLine2: string | null;
  /** @nullable */
  phone: string | null;
  /** @nullable */
  postalCode: string | null;
  /** @nullable */
  recipientName: string | null;
  /** @nullable */
  shippingMemo: string | null;
}

export interface SmartStoreChannelOrderDetailResponse {
  /** @nullable */
  channelCommission: number | null;
  claimDetail: ClaimDetail | null;
  /** @nullable */
  deliveryCompany: string | null;
  deliveryInfo: DeliveryInfo | null;
  /** @nullable */
  expectedDeliveryMethod: string | null;
  /** @nullable */
  expectedSettlementAmount: number | null;
  order: SmartStoreChannelOrderResponse;
  /** @nullable */
  paymentAmount: number | null;
  /** @nullable */
  paymentCommission: number | null;
  /** @nullable */
  placeOrderStatus: string | null;
  /** @nullable */
  saleCommission: number | null;
  /** @nullable */
  shippingDueDate: string | null;
  /** @nullable */
  trackingNumber: string | null;
  /** @nullable */
  unitPrice: number | null;
}

export interface RequestSmartStoreSellerCancelRequest {
  /**
     * @minLength 0
     * @maxLength 500
     */
  detailedReason?: string;
  quantity?: number;
  /** @minLength 1 */
  reason: string;
}

export interface DispatchSmartStoreExchangeRequest {
  /**
     * @minLength 1
     * @pattern ^[A-Z0-9_]{1,40}$
     */
  deliveryCompanyCode: string;
  /**
     * @minLength 1
     * @pattern ^[A-Z_]{1,40}$
     */
  deliveryMethod: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  trackingNumber: string;
}

export interface HoldSmartStoreExchangeRequest {
  /**
     * @minLength 0
     * @maxLength 500
     */
  detailedReason: string;
  extraExchangeFeeAmount?: number;
  /** @minLength 1 */
  holdbackClassType: string;
}

export interface RejectSmartStoreExchangeRequest {
  /**
     * @minLength 0
     * @maxLength 500
     */
  reason: string;
}

export interface DelaySmartStoreOrderRequest {
  /**
     * @minLength 0
     * @maxLength 4000
     */
  detailedReason: string;
  dispatchDueDate: string;
  /**
     * @minLength 1
     * @pattern ^(PRODUCT_PREPARE|CUSTOMER_REQUEST|CUSTOM_BUILD|RESERVED_DISPATCH|OVERSEA_DELIVERY|ETC)$
     */
  reasonCode: string;
}

export interface DispatchSmartStoreOrderRequest {
  /** @pattern ^[A-Z0-9_]{1,40}$ */
  deliveryCompanyCode?: string;
  /**
     * @minLength 1
     * @pattern ^[A-Z_]{1,40}$
     */
  deliveryMethod: string;
  dispatchDate: string;
  /**
     * @minLength 0
     * @maxLength 100
     */
  trackingNumber?: string;
}

export interface ResolveSmartStoreReturnRequest {
  restoreStock: boolean;
}

export type ListOrdersParams = {
status?: ListOrdersStatus;
cursor?: string;
size?: number;
};

export type ListOrdersStatus = typeof ListOrdersStatus[keyof typeof ListOrdersStatus];


export const ListOrdersStatus = {
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

export type SearchOrdersParams = {
status?: SearchOrdersStatus;
dateFrom?: string;
dateTo?: string;
keyword?: string;
page?: number;
size?: number;
};

export type SearchOrdersStatus = typeof SearchOrdersStatus[keyof typeof SearchOrdersStatus];


export const SearchOrdersStatus = {
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

export type ListSmartStoreChannelOrdersParams = {
attentionOnly?: boolean;
/**
 * @minimum 1
 * @maximum 200
 */
limit?: number;
};

export const getListOrdersUrl = (params?: ListOrdersParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/orders?${stringifiedParams}` : `/api/v1/admin/orders`
}

export const listOrders = async (params?: ListOrdersParams, options?: RequestInit): Promise<AdminOrderPageResponse> => {

  return generatedApiClient<AdminOrderPageResponse>(getListOrdersUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getExpirePickupsUrl = () => {




  return `/api/v1/admin/orders/expire-pickups`
}

export const expirePickups = async ( options?: RequestInit): Promise<BatchResponse> => {

  return generatedApiClient<BatchResponse>(getExpirePickupsUrl(),
  {
    ...options,
    method: 'POST'


  }
);}



export const getSearchOrdersUrl = (params?: SearchOrdersParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/orders/search?${stringifiedParams}` : `/api/v1/admin/orders/search`
}

export const searchOrders = async (params?: SearchOrdersParams, options?: RequestInit): Promise<AdminOrderSearchPageResponse> => {

  return generatedApiClient<AdminOrderSearchPageResponse>(getSearchOrdersUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getApproveUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/approve`
}

export const approve = async (id: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getApproveUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getCancelForDelayRejectionUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/cancel-for-delay-rejection`
}

export const cancelForDelayRejection = async (id: number, options?: RequestInit): Promise<OrderDelayCancellationResponse> => {

  return generatedApiClient<OrderDelayCancellationResponse>(getCancelForDelayRejectionUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getConfirmPickupUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/complete-pickup`
}

export const confirmPickup = async (id: number, options?: RequestInit): Promise<PickupResponse> => {

  return generatedApiClient<PickupResponse>(getConfirmPickupUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getCompleteProductionUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/complete-production`
}

export const completeProduction = async (id: number, options?: RequestInit): Promise<OrderProductionResponse> => {

  return generatedApiClient<OrderProductionResponse>(getCompleteProductionUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getProposeDelayUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/delay`
}

export const proposeDelay = async (id: number, options?: RequestInit): Promise<OrderProductionResponse> => {

  return generatedApiClient<OrderProductionResponse>(getProposeDelayUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getSetExpectedShipDateUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/expected-ship-date`
}

export const setExpectedShipDate = async (id: number,
    setExpectedShipDateRequest: SetExpectedShipDateRequest, options?: RequestInit): Promise<OrderProductionResponse> => {

  return generatedApiClient<OrderProductionResponse>(getSetExpectedShipDateUrl(id),
  {
    ...options,
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(setExpectedShipDateRequest)
  }
);}



export const getGetFulfillmentUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/fulfillment`
}

export const getFulfillment = async (id: number, options?: RequestInit): Promise<AdminOrderFulfillmentResponse> => {

  return generatedApiClient<AdminOrderFulfillmentResponse>(getGetFulfillmentUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetOrderHistoryUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/history`
}

export const getOrderHistory = async (id: number, options?: RequestInit): Promise<AdminOrderHistoryResponse[]> => {

  return generatedApiClient<AdminOrderHistoryResponse[]>(getGetOrderHistoryUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getMarkDeliveredUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/mark-delivered`
}

export const markDelivered = async (id: number, options?: RequestInit): Promise<ShippingResponse> => {

  return generatedApiClient<ShippingResponse>(getMarkDeliveredUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getMarkShippedUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/mark-shipped`
}

export const markShipped = async (id: number,
    markShippedRequest: MarkShippedRequest, options?: RequestInit): Promise<ShippingResponse> => {

  return generatedApiClient<ShippingResponse>(getMarkShippedUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(markShippedRequest)
  }
);}



export const getMarkPickupReadyUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/prepare-pickup`
}

export const markPickupReady = async (id: number,
    markPickupReadyRequest: MarkPickupReadyRequest, options?: RequestInit): Promise<PickupResponse> => {

  return generatedApiClient<PickupResponse>(getMarkPickupReadyUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(markPickupReadyRequest)
  }
);}



export const getPrepareShippingUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/prepare-shipping`
}

export const prepareShipping = async (id: number, options?: RequestInit): Promise<ShippingResponse> => {

  return generatedApiClient<ShippingResponse>(getPrepareShippingUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getRefundMissedPickupUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/refund-missed-pickup`
}

export const refundMissedPickup = async (id: number, options?: RequestInit): Promise<MissedPickupRefundResponse> => {

  return generatedApiClient<MissedPickupRefundResponse>(getRefundMissedPickupUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getRejectUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/reject`
}

export const reject = async (id: number, options?: RequestInit): Promise<OrderRejectResponse> => {

  return generatedApiClient<OrderRejectResponse>(getRejectUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getResumeOrderAfterDelayUrl = (id: number,) => {




  return `/api/v1/admin/orders/${id}/resume-after-delay`
}

export const resumeOrderAfterDelay = async (id: number, options?: RequestInit): Promise<OrderProductionResponse> => {

  return generatedApiClient<OrderProductionResponse>(getResumeOrderAfterDelayUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}



export const getListSmartStoreChannelOrdersUrl = (params?: ListSmartStoreChannelOrdersParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/smartstore-orders?${stringifiedParams}` : `/api/v1/admin/smartstore-orders`
}

export const listSmartStoreChannelOrders = async (params?: ListSmartStoreChannelOrdersParams, options?: RequestInit): Promise<SmartStoreChannelOrderResponse[]> => {

  return generatedApiClient<SmartStoreChannelOrderResponse[]>(getListSmartStoreChannelOrdersUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetSmartStoreChannelOrderUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}`
}

export const getSmartStoreChannelOrder = async (productOrderId: string, options?: RequestInit): Promise<SmartStoreChannelOrderDetailResponse> => {

  return generatedApiClient<SmartStoreChannelOrderDetailResponse>(getGetSmartStoreChannelOrderUrl(productOrderId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getApproveSmartStoreCancelClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/cancel/approve`
}

export const approveSmartStoreCancelClaim = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getApproveSmartStoreCancelClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getRequestSmartStoreSellerCancelUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/cancel/request`
}

export const requestSmartStoreSellerCancel = async (productOrderId: string,
    requestSmartStoreSellerCancelRequest: RequestSmartStoreSellerCancelRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRequestSmartStoreSellerCancelUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(requestSmartStoreSellerCancelRequest)
  }
);}



export const getCompleteSmartStoreExchangeCollectUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/exchange/collect/complete`
}

export const completeSmartStoreExchangeCollect = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getCompleteSmartStoreExchangeCollectUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getDispatchSmartStoreExchangeClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/exchange/dispatch`
}

export const dispatchSmartStoreExchangeClaim = async (productOrderId: string,
    dispatchSmartStoreExchangeRequest: DispatchSmartStoreExchangeRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDispatchSmartStoreExchangeClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(dispatchSmartStoreExchangeRequest)
  }
);}



export const getHoldSmartStoreExchangeClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/exchange/hold`
}

export const holdSmartStoreExchangeClaim = async (productOrderId: string,
    holdSmartStoreExchangeRequest: HoldSmartStoreExchangeRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getHoldSmartStoreExchangeClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(holdSmartStoreExchangeRequest)
  }
);}



export const getReleaseSmartStoreExchangeHoldUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/exchange/hold/release`
}

export const releaseSmartStoreExchangeHold = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getReleaseSmartStoreExchangeHoldUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getRejectSmartStoreExchangeClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/exchange/reject`
}

export const rejectSmartStoreExchangeClaim = async (productOrderId: string,
    rejectSmartStoreExchangeRequest: RejectSmartStoreExchangeRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRejectSmartStoreExchangeClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(rejectSmartStoreExchangeRequest)
  }
);}



export const getApproveSmartStoreReturnClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/return/approve`
}

export const approveSmartStoreReturnClaim = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getApproveSmartStoreReturnClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getRejectSmartStoreReturnClaimUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/claims/return/reject`
}

export const rejectSmartStoreReturnClaim = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRejectSmartStoreReturnClaimUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getConfirmSmartStoreChannelOrderUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/confirm`
}

export const confirmSmartStoreChannelOrder = async (productOrderId: string, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getConfirmSmartStoreChannelOrderUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getDelaySmartStoreChannelOrderUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/delay`
}

export const delaySmartStoreChannelOrder = async (productOrderId: string,
    delaySmartStoreOrderRequest: DelaySmartStoreOrderRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDelaySmartStoreChannelOrderUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(delaySmartStoreOrderRequest)
  }
);}



export const getDispatchSmartStoreChannelOrderUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/dispatch`
}

export const dispatchSmartStoreChannelOrder = async (productOrderId: string,
    dispatchSmartStoreOrderRequest: DispatchSmartStoreOrderRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getDispatchSmartStoreChannelOrderUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(dispatchSmartStoreOrderRequest)
  }
);}



export const getRetrySmartStoreChannelOrderInventoryUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/inventory/retry`
}

export const retrySmartStoreChannelOrderInventory = async (productOrderId: string, options?: RequestInit): Promise<SmartStoreChannelOrderResponse> => {

  return generatedApiClient<SmartStoreChannelOrderResponse>(getRetrySmartStoreChannelOrderInventoryUrl(productOrderId),
  {
    ...options,
    method: 'POST'


  }
);}



export const getResolveSmartStoreChannelOrderReturnUrl = (productOrderId: string,) => {




  return `/api/v1/admin/smartstore-orders/${productOrderId}/return-resolution`
}

export const resolveSmartStoreChannelOrderReturn = async (productOrderId: string,
    resolveSmartStoreReturnRequest: ResolveSmartStoreReturnRequest, options?: RequestInit): Promise<SmartStoreChannelOrderResponse> => {

  return generatedApiClient<SmartStoreChannelOrderResponse>(getResolveSmartStoreChannelOrderReturnUrl(productOrderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(resolveSmartStoreReturnRequest)
  }
);}
