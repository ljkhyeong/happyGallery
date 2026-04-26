import { api } from "@/shared/api";
import type { OrderDetailResponse } from "@/shared/types";

export function fetchOrder(id: number, token: string): Promise<OrderDetailResponse> {
  return api<OrderDetailResponse>(`/orders/${id}`, {
    headers: { "X-Access-Token": token },
  });
}
