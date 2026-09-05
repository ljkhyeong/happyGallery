import { generatedApiClient } from '../../shared/api/generatedClient';
export type ProductOptionSnapshotResponseType = typeof ProductOptionSnapshotResponseType[keyof typeof ProductOptionSnapshotResponseType];


export const ProductOptionSnapshotResponseType = {
  SELECT: 'SELECT',
  TEXT: 'TEXT',
} as const;

export interface ProductOptionSnapshotResponse {
  groupName: string;
  priceAdjustment: number;
  sortOrder: number;
  type: ProductOptionSnapshotResponseType;
  value: string;
}

export type CartItemResponseProductType = typeof CartItemResponseProductType[keyof typeof CartItemResponseProductType];


export const CartItemResponseProductType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface CartItemResponse {
  available: boolean;
  basePrice: number;
  /** @nullable */
  careInstructions: string | null;
  cartItemId: number;
  options: ProductOptionSnapshotResponse[];
  price: number;
  productId: number;
  productName: string;
  productType: CartItemResponseProductType;
  /** @nullable */
  productVariantId: number | null;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  /** @nullable */
  specification: string | null;
  subtotal: number;
  textOptionPriceAdjustment: number;
  variantPriceAdjustment: number;
}

export interface CartResponse {
  cartVersion: string;
  items: CartItemResponse[];
  totalAmount: number;
}

export interface ProductTextInputRequest {
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

export interface AddCartItemRequest {
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
  textInputs?: ProductTextInputRequest[];
}

export interface UpdateCartItemRequest {
  /**
     * @minimum 1
     * @maximum 99
     */
  qty: number;
}

export interface MergeCartItemRequest {
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
  textInputs?: ProductTextInputRequest[];
}

export interface MergeCartRequest {
  expectedCustomerId: number;
  idempotencyKey: string;
  /**
     * @minItems 1
     * @maxItems 100
     */
  items: MergeCartItemRequest[];
}

export interface ClaimGuestRecordsRequest {
  /**
     * @minItems 0
     * @maxItems 100
     */
  bookingIds?: number[];
  /**
     * @minItems 0
     * @maxItems 100
     */
  orderIds?: number[];
}

export interface GuestClaimResultResponse {
  claimedBookingCount: number;
  claimedOrderCount: number;
}

export type BookingSummaryStatus = typeof BookingSummaryStatus[keyof typeof BookingSummaryStatus];


export const BookingSummaryStatus = {
  BOOKED: 'BOOKED',
  CANCELED: 'CANCELED',
  NO_SHOW: 'NO_SHOW',
  COMPLETED: 'COMPLETED',
} as const;

export interface BookingSummary {
  bookingId: number;
  className: string;
  endAt: string;
  startAt: string;
  status: BookingSummaryStatus;
}

export type OrderSummaryStatus = typeof OrderSummaryStatus[keyof typeof OrderSummaryStatus];


export const OrderSummaryStatus = {
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

export interface OrderSummary {
  createdAt: string;
  orderId: number;
  status: OrderSummaryStatus;
  totalAmount: number;
}

export interface GuestClaimPreviewResponse {
  bookings: BookingSummary[];
  orders: OrderSummary[];
  phoneVerified: boolean;
}

export interface VerifyGuestClaimPhoneRequest {
  /** @minLength 1 */
  verificationCode: string;
}

export interface InquiryResponse {
  content: string;
  createdAt: string;
  hasReply: boolean;
  id: number;
  /** @nullable */
  repliedAt: string | null;
  /** @nullable */
  replyContent: string | null;
  title: string;
}

export interface CreateInquiryRequest {
  /**
     * @minLength 1
     * @maxLength 16000
     */
  content: string;
  /**
     * @minLength 1
     * @maxLength 200
     */
  title: string;
}

export interface MyInquiryPageResponse {
  content: InquiryResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export type MyOrderSummaryStatus = typeof MyOrderSummaryStatus[keyof typeof MyOrderSummaryStatus];


export const MyOrderSummaryStatus = {
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

export interface MyOrderSummary {
  createdAt: string;
  orderId: number;
  /** @nullable */
  paidAt: string | null;
  status: MyOrderSummaryStatus;
  totalAmount: number;
}

export interface MyOrderPageResponse {
  content: MyOrderSummary[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
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

export type MyPassSummaryPlanCode = typeof MyPassSummaryPlanCode[keyof typeof MyPassSummaryPlanCode];


export const MyPassSummaryPlanCode = {
  LEGACY_ALL_CLASSES: 'LEGACY_ALL_CLASSES',
  REGULAR_CRAFT_8: 'REGULAR_CRAFT_8',
} as const;

export interface MyPassSummary {
  expiresAt: string;
  passId: number;
  planCode: MyPassSummaryPlanCode;
  planName: string;
  purchasedAt: string;
  /** @nullable */
  receiptUrl: string | null;
  refund: RefundProgressResponse | null;
  remainingCredits: number;
  totalCredits: number;
  totalPrice: number;
}

export interface MyPassPageResponse {
  content: MyPassSummary[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

/**
 * @nullable
 */
export type MemberPassRefundResponseRefundStatus = typeof MemberPassRefundResponseRefundStatus[keyof typeof MemberPassRefundResponseRefundStatus] | null;


export const MemberPassRefundResponseRefundStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export interface MemberPassRefundResponse {
  canceledBookings: number;
  refundAmount: number;
  refundCredits: number;
  /** @nullable */
  refundStatus: MemberPassRefundResponseRefundStatus;
}

export type ListMyInquiriesPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

export type ListMyOrdersPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
keyword?: string;
status?: ListMyOrdersPageStatus;
sort?: ListMyOrdersPageSort;
};

export type ListMyOrdersPageStatus = typeof ListMyOrdersPageStatus[keyof typeof ListMyOrdersPageStatus];


export const ListMyOrdersPageStatus = {
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

export type ListMyOrdersPageSort = typeof ListMyOrdersPageSort[keyof typeof ListMyOrdersPageSort];


export const ListMyOrdersPageSort = {
  LATEST: 'LATEST',
  OLDEST: 'OLDEST',
  AMOUNT_DESC: 'AMOUNT_DESC',
  AMOUNT_ASC: 'AMOUNT_ASC',
} as const;

export type ListMyPassesPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
keyword?: string;
status?: ListMyPassesPageStatus;
sort?: ListMyPassesPageSort;
};

export type ListMyPassesPageStatus = typeof ListMyPassesPageStatus[keyof typeof ListMyPassesPageStatus];


export const ListMyPassesPageStatus = {
  ACTIVE: 'ACTIVE',
  USED_UP: 'USED_UP',
  EXPIRED: 'EXPIRED',
} as const;

export type ListMyPassesPageSort = typeof ListMyPassesPageSort[keyof typeof ListMyPassesPageSort];


export const ListMyPassesPageSort = {
  PURCHASE_DESC: 'PURCHASE_DESC',
  EXPIRY_ASC: 'EXPIRY_ASC',
  CREDITS_DESC: 'CREDITS_DESC',
} as const;

export const getGetMyCartUrl = () => {




  return `/api/v1/me/cart`
}

export const getMyCart = async ( options?: RequestInit): Promise<CartResponse> => {

  return generatedApiClient<CartResponse>(getGetMyCartUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getAddMyCartItemUrl = () => {




  return `/api/v1/me/cart/items`
}

export const addMyCartItem = async (addCartItemRequest: AddCartItemRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getAddMyCartItemUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(addCartItemRequest)
  }
);}



export const getRemoveMyCartItemUrl = (cartItemId: number,) => {




  return `/api/v1/me/cart/items/${cartItemId}`
}

export const removeMyCartItem = async (cartItemId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRemoveMyCartItemUrl(cartItemId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getUpdateMyCartItemQuantityUrl = (cartItemId: number,) => {




  return `/api/v1/me/cart/items/${cartItemId}`
}

export const updateMyCartItemQuantity = async (cartItemId: number,
    updateCartItemRequest: UpdateCartItemRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUpdateMyCartItemQuantityUrl(cartItemId),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateCartItemRequest)
  }
);}



export const getMergeMyCartItemsUrl = () => {




  return `/api/v1/me/cart/merge`
}

export const mergeMyCartItems = async (mergeCartRequest: MergeCartRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getMergeMyCartItemsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(mergeCartRequest)
  }
);}



export const getClaimGuestRecordsUrl = () => {




  return `/api/v1/me/guest-claims`
}

export const claimGuestRecords = async (claimGuestRecordsRequest: ClaimGuestRecordsRequest, options?: RequestInit): Promise<GuestClaimResultResponse> => {

  return generatedApiClient<GuestClaimResultResponse>(getClaimGuestRecordsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(claimGuestRecordsRequest)
  }
);}



export const getPreviewGuestClaimsUrl = () => {




  return `/api/v1/me/guest-claims/preview`
}

export const previewGuestClaims = async ( options?: RequestInit): Promise<GuestClaimPreviewResponse> => {

  return generatedApiClient<GuestClaimPreviewResponse>(getPreviewGuestClaimsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getVerifyPhoneAndPreviewGuestClaimsUrl = () => {




  return `/api/v1/me/guest-claims/verify`
}

export const verifyPhoneAndPreviewGuestClaims = async (verifyGuestClaimPhoneRequest: VerifyGuestClaimPhoneRequest, options?: RequestInit): Promise<GuestClaimPreviewResponse> => {

  return generatedApiClient<GuestClaimPreviewResponse>(getVerifyPhoneAndPreviewGuestClaimsUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(verifyGuestClaimPhoneRequest)
  }
);}



export const getListMyInquiriesUrl = () => {




  return `/api/v1/me/inquiries`
}

export const listMyInquiries = async ( options?: RequestInit): Promise<InquiryResponse[]> => {

  return generatedApiClient<InquiryResponse[]>(getListMyInquiriesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCreateMyInquiryUrl = () => {




  return `/api/v1/me/inquiries`
}

export const createMyInquiry = async (createInquiryRequest: CreateInquiryRequest, options?: RequestInit): Promise<InquiryResponse> => {

  return generatedApiClient<InquiryResponse>(getCreateMyInquiryUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(createInquiryRequest)
  }
);}



export const getListMyInquiriesPageUrl = (params?: ListMyInquiriesPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/inquiries/page?${stringifiedParams}` : `/api/v1/me/inquiries/page`
}

export const listMyInquiriesPage = async (params?: ListMyInquiriesPageParams, options?: RequestInit): Promise<MyInquiryPageResponse> => {

  return generatedApiClient<MyInquiryPageResponse>(getListMyInquiriesPageUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetMyInquiryUrl = (id: number,) => {




  return `/api/v1/me/inquiries/${id}`
}

export const getMyInquiry = async (id: number, options?: RequestInit): Promise<InquiryResponse> => {

  return generatedApiClient<InquiryResponse>(getGetMyInquiryUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyOrdersUrl = () => {




  return `/api/v1/me/orders`
}

export const listMyOrders = async ( options?: RequestInit): Promise<MyOrderSummary[]> => {

  return generatedApiClient<MyOrderSummary[]>(getListMyOrdersUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyOrdersPageUrl = (params?: ListMyOrdersPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/orders/page?${stringifiedParams}` : `/api/v1/me/orders/page`
}

export const listMyOrdersPage = async (params?: ListMyOrdersPageParams, options?: RequestInit): Promise<MyOrderPageResponse> => {

  return generatedApiClient<MyOrderPageResponse>(getListMyOrdersPageUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCancelMyOrderUrl = (id: number,) => {




  return `/api/v1/me/orders/${id}`
}

export const cancelMyOrder = async (id: number, options?: RequestInit): Promise<OrderCustomerActionResponse> => {

  return generatedApiClient<OrderCustomerActionResponse>(getCancelMyOrderUrl(id),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getGetMyOrderUrl = (id: number,) => {




  return `/api/v1/me/orders/${id}`
}

export const getMyOrder = async (id: number, options?: RequestInit): Promise<OrderDetailResponse> => {

  return generatedApiClient<OrderDetailResponse>(getGetMyOrderUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRespondToMyOrderDelayUrl = (id: number,) => {




  return `/api/v1/me/orders/${id}/delay-response`
}

export const respondToMyOrderDelay = async (id: number,
    orderDelayResponseRequest: OrderDelayResponseRequest, options?: RequestInit): Promise<OrderCustomerActionResponse> => {

  return generatedApiClient<OrderCustomerActionResponse>(getRespondToMyOrderDelayUrl(id),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(orderDelayResponseRequest)
  }
);}



export const getUpdateMyOrderShippingAddressUrl = (id: number,) => {




  return `/api/v1/me/orders/${id}/shipping-address`
}

export const updateMyOrderShippingAddress = async (id: number,
    updateShippingAddressRequest: UpdateShippingAddressRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUpdateMyOrderShippingAddressUrl(id),
  {
    ...options,
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(updateShippingAddressRequest)
  }
);}



export const getListMyPassesUrl = () => {




  return `/api/v1/me/passes`
}

export const listMyPasses = async ( options?: RequestInit): Promise<MyPassSummary[]> => {

  return generatedApiClient<MyPassSummary[]>(getListMyPassesUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getListMyPassesPageUrl = (params?: ListMyPassesPageParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/me/passes/page?${stringifiedParams}` : `/api/v1/me/passes/page`
}

export const listMyPassesPage = async (params?: ListMyPassesPageParams, options?: RequestInit): Promise<MyPassPageResponse> => {

  return generatedApiClient<MyPassPageResponse>(getListMyPassesPageUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetMyPassUrl = (id: number,) => {




  return `/api/v1/me/passes/${id}`
}

export const getMyPass = async (id: number, options?: RequestInit): Promise<MyPassSummary> => {

  return generatedApiClient<MyPassSummary>(getGetMyPassUrl(id),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRefundMyPassUrl = (id: number,) => {




  return `/api/v1/me/passes/${id}/refund`
}

export const refundMyPass = async (id: number, options?: RequestInit): Promise<MemberPassRefundResponse> => {

  return generatedApiClient<MemberPassRefundResponse>(getRefundMyPassUrl(id),
  {
    ...options,
    method: 'POST'


  }
);}
