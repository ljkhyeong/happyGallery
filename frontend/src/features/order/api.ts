import { api } from "@/shared/api";
import type { CreateOrderRequest, OrderResponse, OrderDetailResponse } from "@/shared/types";

export function createOrder(body: CreateOrderRequest): Promise<OrderResponse> {
  return api<OrderResponse>("/orders", { method: "POST", body });
}

export function fetchOrder(id: number, token: string): Promise<OrderDetailResponse> {
  return api<OrderDetailResponse>(`/orders/${id}`, { params: { token } });
}
