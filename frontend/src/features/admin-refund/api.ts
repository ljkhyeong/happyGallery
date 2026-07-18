import { adminHeaders as h, api } from "@/shared/api";
import type { FailedRefundResponse } from "@/shared/types";
import type { AdminRefundStatus } from "@/shared/types";

export function fetchFailedRefunds(adminKey: string): Promise<FailedRefundResponse[]> {
  return api("/admin/refunds/failed", { headers: h(adminKey) });
}

export function fetchRefund(adminKey: string, refundId: number): Promise<AdminRefundStatus> {
  return api(`/admin/refunds/${refundId}`, { headers: h(adminKey) });
}

export function retryRefund(adminKey: string, refundId: number): Promise<AdminRefundStatus> {
  return api(`/admin/refunds/${refundId}/retry`, { method: "POST", headers: h(adminKey) });
}
