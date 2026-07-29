import {
  getAdminPass as getPass,
  refundPass as requestPassRefund,
  searchAdminPasses as searchPasses,
  triggerExpiry,
  type AdminPassPageResponse,
  type AdminPassResponse,
  type BatchResponse,
  type PassRefundResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export function searchAdminPasses(
  adminKey: string,
  keyword: string | undefined,
  page: number,
  size: number,
): Promise<AdminPassPageResponse> {
  return searchPasses(
    { keyword, page, size },
    { headers: adminHeaders(adminKey) },
  );
}

export function getAdminPass(
  adminKey: string,
  passId: number,
): Promise<AdminPassResponse> {
  return getPass(passId, { headers: adminHeaders(adminKey) });
}

export function expirePasses(adminKey: string): Promise<BatchResponse> {
  return triggerExpiry({ headers: adminHeaders(adminKey) });
}

export function refundPass(
  adminKey: string,
  passId: number,
): Promise<PassRefundResponse> {
  return requestPassRefund(passId, { headers: adminHeaders(adminKey) });
}
