import { generatedApiClient } from '../../shared/api/generatedClient';
export interface ClaimedItemResponse {
  orderItemId: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

/**
 * @nullable
 */
export type OrderClaimResponseRefundStatus = typeof OrderClaimResponseRefundStatus[keyof typeof OrderClaimResponseRefundStatus] | null;


export const OrderClaimResponseRefundStatus = {
  REQUESTED: 'REQUESTED',
  PROCESSING: 'PROCESSING',
  RETRYABLE: 'RETRYABLE',
  RECONCILIATION_REQUIRED: 'RECONCILIATION_REQUIRED',
  SUCCEEDED: 'SUCCEEDED',
  FAILED: 'FAILED',
} as const;

export type OrderClaimResponseRequestedResolution = typeof OrderClaimResponseRequestedResolution[keyof typeof OrderClaimResponseRequestedResolution];


export const OrderClaimResponseRequestedResolution = {
  REFUND: 'REFUND',
  EXCHANGE: 'EXCHANGE',
} as const;

export type OrderClaimResponseStatus = typeof OrderClaimResponseStatus[keyof typeof OrderClaimResponseStatus];


export const OrderClaimResponseStatus = {
  REQUESTED: 'REQUESTED',
  REFUND_REQUESTED: 'REFUND_REQUESTED',
  EXCHANGE_APPROVED: 'EXCHANGE_APPROVED',
  REJECTED: 'REJECTED',
  COMPLETED: 'COMPLETED',
} as const;

export type OrderClaimResponseType = typeof OrderClaimResponseType[keyof typeof OrderClaimResponseType];


export const OrderClaimResponseType = {
  DAMAGED: 'DAMAGED',
  WRONG_ITEM: 'WRONG_ITEM',
  CHANGE_OF_MIND: 'CHANGE_OF_MIND',
  OTHER: 'OTHER',
} as const;

export interface OrderClaimResponse {
  /** @nullable */
  adminNote: string | null;
  /** @nullable */
  completedAt: string | null;
  /** @nullable */
  completedByAdminId: number | null;
  customerReason: string;
  id: number;
  items: ClaimedItemResponse[];
  maximumRefundAmount: number;
  orderId: number;
  /** @nullable */
  refundAmount: number | null;
  /** @nullable */
  refundStatus: OrderClaimResponseRefundStatus;
  /** @nullable */
  replacementCarrier: string | null;
  /** @nullable */
  replacementTrackingNumber: string | null;
  requestedAt: string;
  requestedResolution: OrderClaimResponseRequestedResolution;
  /** @nullable */
  resolvedAt: string | null;
  /** @nullable */
  resolvedByAdminId: number | null;
  status: OrderClaimResponseStatus;
  type: OrderClaimResponseType;
}

export interface AdminOrderClaimPageResponse {
  content: OrderClaimResponse[];
  hasMore: boolean;
  /** @nullable */
  nextCursor: string | null;
}

export interface CompleteOrderExchangeRequest {
  /**
     * @minLength 1
     * @maxLength 100
     */
  carrier: string;
  /**
     * @minLength 0
     * @maxLength 1000
     */
  note?: string;
  /**
     * @minLength 1
     * @maxLength 100
     */
  trackingNumber: string;
}

export interface ResolveOrderClaimRequest {
  approved: boolean;
  /**
     * @minLength 0
     * @maxLength 1000
     */
  note?: string;
  refundAmount?: number;
  restoreInventory: boolean;
}

export type OrderClaimRequestRequestedResolution = typeof OrderClaimRequestRequestedResolution[keyof typeof OrderClaimRequestRequestedResolution];


export const OrderClaimRequestRequestedResolution = {
  REFUND: 'REFUND',
  EXCHANGE: 'EXCHANGE',
} as const;

export type OrderClaimRequestType = typeof OrderClaimRequestType[keyof typeof OrderClaimRequestType];


export const OrderClaimRequestType = {
  DAMAGED: 'DAMAGED',
  WRONG_ITEM: 'WRONG_ITEM',
  CHANGE_OF_MIND: 'CHANGE_OF_MIND',
  OTHER: 'OTHER',
} as const;

export interface ClaimItemRequest {
  orderItemId: number;
  quantity: number;
}

export interface OrderClaimRequest {
  /**
     * @minItems 1
     * @maxItems 100
     */
  items: ClaimItemRequest[];
  /**
     * @minLength 0
     * @maxLength 1000
     */
  reason: string;
  requestedResolution: OrderClaimRequestRequestedResolution;
  type: OrderClaimRequestType;
}

export type ListAdminOrderClaimsParams = {
status?: ListAdminOrderClaimsStatus;
cursor?: string;
size?: number;
};

export type ListAdminOrderClaimsStatus = typeof ListAdminOrderClaimsStatus[keyof typeof ListAdminOrderClaimsStatus];


export const ListAdminOrderClaimsStatus = {
  REQUESTED: 'REQUESTED',
  REFUND_REQUESTED: 'REFUND_REQUESTED',
  EXCHANGE_APPROVED: 'EXCHANGE_APPROVED',
  REJECTED: 'REJECTED',
  COMPLETED: 'COMPLETED',
} as const;

export const getListAdminOrderClaimsUrl = (params?: ListAdminOrderClaimsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/order-claims?${stringifiedParams}` : `/api/v1/admin/order-claims`
}

export const listAdminOrderClaims = async (params?: ListAdminOrderClaimsParams, options?: RequestInit): Promise<AdminOrderClaimPageResponse> => {

  return generatedApiClient<AdminOrderClaimPageResponse>(getListAdminOrderClaimsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getCompleteOrderClaimExchangeUrl = (claimId: number,) => {




  return `/api/v1/admin/order-claims/${claimId}/complete-exchange`
}

export const completeOrderClaimExchange = async (claimId: number,
    completeOrderExchangeRequest: CompleteOrderExchangeRequest, options?: RequestInit): Promise<OrderClaimResponse> => {

  return generatedApiClient<OrderClaimResponse>(getCompleteOrderClaimExchangeUrl(claimId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(completeOrderExchangeRequest)
  }
);}



export const getResolveOrderClaimUrl = (claimId: number,) => {




  return `/api/v1/admin/order-claims/${claimId}/resolve`
}

export const resolveOrderClaim = async (claimId: number,
    resolveOrderClaimRequest: ResolveOrderClaimRequest, options?: RequestInit): Promise<OrderClaimResponse> => {

  return generatedApiClient<OrderClaimResponse>(getResolveOrderClaimUrl(claimId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(resolveOrderClaimRequest)
  }
);}



export const getListMyOrderClaimsUrl = (orderId: number,) => {




  return `/api/v1/me/orders/${orderId}/claims`
}

export const listMyOrderClaims = async (orderId: number, options?: RequestInit): Promise<OrderClaimResponse[]> => {

  return generatedApiClient<OrderClaimResponse[]>(getListMyOrderClaimsUrl(orderId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRequestMyOrderClaimUrl = (orderId: number,) => {




  return `/api/v1/me/orders/${orderId}/claims`
}

export const requestMyOrderClaim = async (orderId: number,
    orderClaimRequest: OrderClaimRequest, options?: RequestInit): Promise<OrderClaimResponse> => {

  return generatedApiClient<OrderClaimResponse>(getRequestMyOrderClaimUrl(orderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(orderClaimRequest)
  }
);}



export const getListGuestOrderClaimsUrl = (orderId: number,) => {




  return `/api/v1/orders/${orderId}/claims`
}

export const listGuestOrderClaims = async (orderId: number, options?: RequestInit): Promise<OrderClaimResponse[]> => {

  return generatedApiClient<OrderClaimResponse[]>(getListGuestOrderClaimsUrl(orderId),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRequestGuestOrderClaimUrl = (orderId: number,) => {




  return `/api/v1/orders/${orderId}/claims`
}

export const requestGuestOrderClaim = async (orderId: number,
    orderClaimRequest: OrderClaimRequest, options?: RequestInit): Promise<OrderClaimResponse> => {

  return generatedApiClient<OrderClaimResponse>(getRequestGuestOrderClaimUrl(orderId),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(orderClaimRequest)
  }
);}
