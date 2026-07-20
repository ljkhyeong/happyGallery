import { adminHeaders as h, api } from "@/shared/api";
import type { AdminRefundStatus, CursorPage, FailedRefundResponse } from "@/shared/types";

export function fetchFailedRefunds(
  adminKey: string,
  cursor?: string,
): Promise<CursorPage<FailedRefundResponse>> {
  return api("/admin/refunds/failed", {
    headers: h(adminKey),
    params: { cursor, size: "20" },
  });
}

export function fetchRefund(adminKey: string, refundId: number): Promise<AdminRefundStatus> {
  return api(`/admin/refunds/${refundId}`, { headers: h(adminKey) });
}

export function retryRefund(adminKey: string, refundId: number): Promise<AdminRefundStatus> {
  return api(`/admin/refunds/${refundId}/retry`, { method: "POST", headers: h(adminKey) });
}
