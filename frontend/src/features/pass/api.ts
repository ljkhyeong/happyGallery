import { api } from "@/shared/api";
import type { PurchasePassByPhoneRequest, PurchasePassResponse } from "@/shared/types";

export function purchasePassByPhone(body: PurchasePassByPhoneRequest): Promise<PurchasePassResponse> {
  return api<PurchasePassResponse>("/passes/purchase", { method: "POST", body });
}
