import { adminHeaders as h, api } from "@/shared/api";
import type { AdminPassResponse, BatchResponse, OffsetPage, PassRefundResponse } from "@/shared/types";

export function searchAdminPasses(
  adminKey: string,
  keyword: string | undefined,
  page: number,
  size: number,
): Promise<OffsetPage<AdminPassResponse>> {
  return api<OffsetPage<AdminPassResponse>>("/admin/passes/search", {
    headers: h(adminKey),
    params: { keyword, page, size },
  });
}

export function getAdminPass(adminKey: string, passId: number): Promise<AdminPassResponse> {
  return api<AdminPassResponse>(`/admin/passes/${passId}`, { headers: h(adminKey) });
}

export function expirePasses(adminKey: string): Promise<BatchResponse> {
  return api("/admin/passes/expire", { method: "POST", headers: h(adminKey) });
}

export function refundPass(adminKey: string, passId: number): Promise<PassRefundResponse> {
  return api(`/admin/passes/${passId}/refund`, { method: "POST", headers: h(adminKey) });
}
