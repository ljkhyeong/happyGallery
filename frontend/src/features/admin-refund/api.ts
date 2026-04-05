import { adminHeaders as h, api } from "@/shared/api";
import type { FailedRefundResponse } from "@/shared/types";

export function fetchFailedRefunds(adminKey: string): Promise<FailedRefundResponse[]> {
  return api("/admin/refunds/failed", { headers: h(adminKey) });
}

export function retryRefund(adminKey: string, refundId: number): Promise<void> {
  return api(`/admin/refunds/${refundId}/retry`, { method: "POST", headers: h(adminKey) });
}
