import { generatedApiClient } from '../../shared/api/generatedClient';
export type CartItemResponseProductType = typeof CartItemResponseProductType[keyof typeof CartItemResponseProductType];


export const CartItemResponseProductType = {
  READY_STOCK: 'READY_STOCK',
  MADE_TO_ORDER: 'MADE_TO_ORDER',
} as const;

export interface CartItemResponse {
  available: boolean;
  /** @nullable */
  careInstructions: string | null;
  price: number;
  productId: number;
  productName: string;
  productType: CartItemResponseProductType;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  /** @nullable */
  specification: string | null;
  subtotal: number;
}

export interface CartResponse {
  cartVersion: string;
  items: CartItemResponse[];
  totalAmount: number;
}

export interface AddCartItemRequest {
  productId: number;
  /**
     * @minimum 1
     * @maximum 99
     */
  qty: number;
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
  /**
     * @minimum 1
     * @maximum 99
     */
  qty: number;
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
  couponDiscountAmount: number;
  grossAmount: number;
  netPaidAmount: number;
  orderItemId: number;
  productId: number;
  productName: string;
  /** @nullable */
  productType: ItemDtoProductType;
  /** @nullable */
  productionLeadDays: number | null;
  qty: number;
  rewardUsedAmount: number;
  /** @nullable */
  specification: string | null;
  unitPrice: number;
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
};

export type ListMyPassesPageParams = {
cursor?: string;
/**
 * @minimum 1
 * @maximum 100
 */
size?: number;
};

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



export const getRemoveMyCartItemUrl = (productId: number,) => {




  return `/api/v1/me/cart/items/${productId}`
}

export const removeMyCartItem = async (productId: number, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getRemoveMyCartItemUrl(productId),
  {
    ...options,
    method: 'DELETE'


  }
);}



export const getUpdateMyCartItemQuantityUrl = (productId: number,) => {




  return `/api/v1/me/cart/items/${productId}`
}

export const updateMyCartItemQuantity = async (productId: number,
    updateCartItemRequest: UpdateCartItemRequest, options?: RequestInit): Promise<void> => {

  return generatedApiClient<void>(getUpdateMyCartItemQuantityUrl(productId),
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
