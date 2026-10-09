import {
  createAdminCoupon,
  deleteAdminCoupon,
  getAdminCoupon,
  listAdminCoupons,
  updateAdminCoupon,
  type AdminCouponResponse,
  type CreateCouponRequest,
  type UpdateCouponRequest,
} from "@/generated/api/adminCoupon";
import { queryOptions } from "@tanstack/react-query";
import { adminHeaders, queryKeys } from "@/shared/api";

export type {
  AdminCouponResponse,
  CreateCouponRequest,
  UpdateCouponRequest,
} from "@/generated/api/adminCoupon";

export function fetchAdminCoupons(token: string): Promise<AdminCouponResponse[]> {
  return listAdminCoupons({ headers: adminHeaders(token) });
}

/** 쿠폰 관리와 이벤트 쿠폰 연결이 같은 캐시로 공유하는 쿠폰 목록 조회 */
export function adminCouponsQuery(token: string) {
  return queryOptions({
    queryKey: queryKeys.admin.coupons,
    queryFn: () => fetchAdminCoupons(token),
  });
}

export function fetchAdminCoupon(
  id: number,
  token: string,
): Promise<AdminCouponResponse> {
  return getAdminCoupon(id, { headers: adminHeaders(token) });
}

export function createCoupon(
  request: CreateCouponRequest,
  token: string,
): Promise<AdminCouponResponse> {
  return createAdminCoupon(request, { headers: adminHeaders(token) });
}

export function updateCoupon(
  id: number,
  request: UpdateCouponRequest,
  token: string,
): Promise<AdminCouponResponse> {
  return updateAdminCoupon(id, request, { headers: adminHeaders(token) });
}

export function deleteCoupon(
  id: number,
  expectedVersion: number,
  token: string,
): Promise<void> {
  return deleteAdminCoupon(
    id,
    { expectedVersion },
    { headers: adminHeaders(token) },
  );
}
