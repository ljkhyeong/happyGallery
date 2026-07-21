import { api } from "@/shared/api";
import type { OrderCustomerActionResponse, OrderDelayDecision, OrderDetailResponse, OrderPricePolicyResponse } from "@/shared/types";

export function fetchOrderPricePolicy(): Promise<OrderPricePolicyResponse> {
  return api<OrderPricePolicyResponse>("/orders/policy");
}

export function fetchOrder(id: number, token: string): Promise<OrderDetailResponse> {
  return api<OrderDetailResponse>(`/orders/${id}`, {
    headers: { "X-Access-Token": token },
  });
}

export function cancelGuestOrder(id: number, token: string): Promise<OrderCustomerActionResponse> {
  return api<OrderCustomerActionResponse>(`/orders/${id}`, {
    method: "DELETE",
    headers: { "X-Access-Token": token },
  });
}

export function respondToGuestOrderDelay(
  id: number,
  token: string,
  decision: OrderDelayDecision,
): Promise<OrderCustomerActionResponse> {
  return api<OrderCustomerActionResponse>(`/orders/${id}/delay-response`, {
    method: "POST",
    headers: { "X-Access-Token": token },
    body: { decision },
  });
}

export function cancelMyOrder(id: number): Promise<OrderCustomerActionResponse> {
  return api<OrderCustomerActionResponse>(`/me/orders/${id}`, { method: "DELETE" });
}

export function respondToMyOrderDelay(
  id: number,
  decision: OrderDelayDecision,
): Promise<OrderCustomerActionResponse> {
  return api<OrderCustomerActionResponse>(`/me/orders/${id}/delay-response`, {
    method: "POST",
    body: { decision },
  });
}
