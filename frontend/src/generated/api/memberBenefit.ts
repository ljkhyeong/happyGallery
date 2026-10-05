import { generatedApiClient } from '../../shared/api/generatedClient';
export type MyCouponResponseDiscountType = typeof MyCouponResponseDiscountType[keyof typeof MyCouponResponseDiscountType];


export const MyCouponResponseDiscountType = {
  FIXED: 'FIXED',
  PERCENT: 'PERCENT',
} as const;

export type MyCouponResponseStatus = typeof MyCouponResponseStatus[keyof typeof MyCouponResponseStatus];


export const MyCouponResponseStatus = {
  AVAILABLE: 'AVAILABLE',
  RESERVED: 'RESERVED',
  REDEEMED: 'REDEEMED',
  EXPIRED: 'EXPIRED',
  CANCELED: 'CANCELED',
} as const;

export interface MyCouponResponse {
  claimedAt: string;
  definitionId: number;
  discountType: MyCouponResponseDiscountType;
  discountValue: number;
  id: number;
  /** @nullable */
  maxDiscountAmount: number | null;
  minOrderAmount: number;
  name: string;
  /** @nullable */
  reservedAt: string | null;
  status: MyCouponResponseStatus;
  /** @nullable */
  usedAt: string | null;
  validFrom: string;
  validUntil: string;
}

export interface ClaimCouponRequest {
  definitionId: number;
}

export type ClaimableCouponResponseDiscountType = typeof ClaimableCouponResponseDiscountType[keyof typeof ClaimableCouponResponseDiscountType];


export const ClaimableCouponResponseDiscountType = {
  FIXED: 'FIXED',
  PERCENT: 'PERCENT',
} as const;

export interface ClaimableCouponResponse {
  definitionId: number;
  discountType: ClaimableCouponResponseDiscountType;
  discountValue: number;
  /** @nullable */
  maxDiscountAmount: number | null;
  minOrderAmount: number;
  name: string;
  validFrom: string;
  validUntil: string;
}

export type RewardHistoryResponseType = typeof RewardHistoryResponseType[keyof typeof RewardHistoryResponseType];


export const RewardHistoryResponseType = {
  EARN: 'EARN',
  RESERVE: 'RESERVE',
  RELEASE: 'RELEASE',
  USE: 'USE',
  RESTORE: 'RESTORE',
  EXPIRE: 'EXPIRE',
  REVOKE: 'REVOKE',
  ADJUST: 'ADJUST',
} as const;

export interface RewardHistoryResponse {
  amount: number;
  availableAfter: number;
  createdAt: string;
  debtAfter: number;
  id: number;
  /** @nullable */
  orderId: number | null;
  reservedAfter: number;
  type: RewardHistoryResponseType;
}

export interface RewardWalletResponse {
  availableBalance: number;
  debtBalance: number;
  history: RewardHistoryResponse[];
  reservedBalance: number;
}

export const getListMyCouponsUrl = () => {




  return `/api/v1/me/coupons`
}

/**
 * 최근 발급 100개와 사용 가능·결제 처리 중인 모든 쿠폰을 중복 없이 최신 발급순으로 조회한다.
 */
export const listMyCoupons = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<MyCouponResponse[]> => {

  return generatedApiClient<MyCouponResponse[]>(getListMyCouponsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getClaimMyCouponUrl = () => {




  return `/api/v1/me/coupons`
}

export const claimMyCoupon = async (claimCouponRequest: ClaimCouponRequest, options?: Parameters<typeof generatedApiClient>[1]): Promise<MyCouponResponse> => {

    const getHeaders = (h?: NonNullable<RequestInit['headers']>): Record<string, string | readonly string[]> => {
    if (!h) return {};
    if (h instanceof Headers) return Object.fromEntries(h.entries());
    if (Symbol.iterator in h) {
      return Object.fromEntries(
        Array.from(h as Iterable<Iterable<string>>, (entry) => Array.from(entry) as [string, string]),
      );
    }
    const headers: Record<string, string | readonly string[]> = {};
    for (const [name, value] of Object.entries<string | readonly string[] | undefined>(h)) {
      if (value !== undefined) headers[name] = value;
    }
    return headers;
  };
return generatedApiClient<MyCouponResponse>(getClaimMyCouponUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...getHeaders(options?.headers) },
    body: JSON.stringify(claimCouponRequest)
  }
);}



export const getListClaimableCouponsUrl = () => {




  return `/api/v1/me/coupons/claimable`
}

export const listClaimableCoupons = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<ClaimableCouponResponse[]> => {

  return generatedApiClient<ClaimableCouponResponse[]>(getListClaimableCouponsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetMyRewardWalletUrl = () => {




  return `/api/v1/me/rewards`
}

export const getMyRewardWallet = async ( options?: Parameters<typeof generatedApiClient>[1]): Promise<RewardWalletResponse> => {

  return generatedApiClient<RewardWalletResponse>(getGetMyRewardWalletUrl(),
  {
    ...options,
    method: 'GET'


  }
);}
