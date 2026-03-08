import { api } from "@/shared/api";
import type { BatchResponse, PassRefundResponse } from "@/shared/types";

function h(adminKey: string) {
  return { "X-Admin-Key": adminKey };
}

export function expirePasses(adminKey: string): Promise<BatchResponse> {
  return api("/admin/passes/expire", { method: "POST", headers: h(adminKey) });
}

export function refundPass(adminKey: string, passId: number): Promise<PassRefundResponse> {
  return api(`/admin/passes/${passId}/refund`, { method: "POST", headers: h(adminKey) });
}
