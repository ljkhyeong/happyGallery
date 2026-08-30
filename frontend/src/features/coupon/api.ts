import {
  claimMyCoupon,
  listClaimableCoupons,
  listMyCoupons,
  type ClaimableCouponResponse,
  type MyCouponResponse,
} from "@/generated/api/memberBenefit";

export type {
  ClaimableCouponResponse,
  MyCouponResponse,
} from "@/generated/api/memberBenefit";

export function fetchMyCoupons(signal?: AbortSignal): Promise<MyCouponResponse[]> {
  return listMyCoupons({ signal });
}

export function fetchClaimableCoupons(
  signal?: AbortSignal,
): Promise<ClaimableCouponResponse[]> {
  return listClaimableCoupons({ signal });
}

export function claimCoupon(definitionId: number): Promise<MyCouponResponse> {
  return claimMyCoupon({ definitionId });
}
