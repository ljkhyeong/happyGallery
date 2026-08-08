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

export const listMyCoupons = async ( options?: RequestInit): Promise<MyCouponResponse[]> => {

  return generatedApiClient<MyCouponResponse[]>(getListMyCouponsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getClaimMyCouponUrl = () => {




  return `/api/v1/me/coupons`
}

export const claimMyCoupon = async (claimCouponRequest: ClaimCouponRequest, options?: RequestInit): Promise<MyCouponResponse> => {

  return generatedApiClient<MyCouponResponse>(getClaimMyCouponUrl(),
  {
    ...options,
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    body: JSON.stringify(claimCouponRequest)
  }
);}



export const getListClaimableCouponsUrl = () => {




  return `/api/v1/me/coupons/claimable`
}

export const listClaimableCoupons = async ( options?: RequestInit): Promise<ClaimableCouponResponse[]> => {

  return generatedApiClient<ClaimableCouponResponse[]>(getListClaimableCouponsUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getGetMyRewardWalletUrl = () => {




  return `/api/v1/me/rewards`
}

export const getMyRewardWallet = async ( options?: RequestInit): Promise<RewardWalletResponse> => {

  return generatedApiClient<RewardWalletResponse>(getGetMyRewardWalletUrl(),
  {
    ...options,
    method: 'GET'


  }
);}
