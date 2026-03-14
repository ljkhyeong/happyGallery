import { api } from "@/shared/api";
import type { AdminOrderResponse, OrderProductionResponse, PickupResponse, BatchResponse, SetExpectedShipDateRequest, MarkPickupReadyRequest } from "@/shared/types";

function h(token: string) {
  return { Authorization: `Bearer ${token}` };
}

export function fetchOrders(
  adminKey: string,
  status?: string,
): Promise<AdminOrderResponse[]> {
  return api<AdminOrderResponse[]>("/admin/orders", {
    headers: h(adminKey),
    params: { status },
  });
}

export function approveOrder(adminKey: string, id: number): Promise<void> {
  return api(`/admin/orders/${id}/approve`, { method: "POST", headers: h(adminKey) });
}

export function rejectOrder(adminKey: string, id: number): Promise<void> {
  return api(`/admin/orders/${id}/reject`, { method: "POST", headers: h(adminKey) });
}

export function completeProduction(adminKey: string, id: number): Promise<OrderProductionResponse> {
  return api(`/admin/orders/${id}/complete-production`, { method: "POST", headers: h(adminKey) });
}

export function setExpectedShipDate(adminKey: string, id: number, body: SetExpectedShipDateRequest): Promise<OrderProductionResponse> {
  return api(`/admin/orders/${id}/expected-ship-date`, { method: "PATCH", headers: h(adminKey), body });
}

export function requestDelay(adminKey: string, id: number): Promise<OrderProductionResponse> {
  return api(`/admin/orders/${id}/delay`, { method: "POST", headers: h(adminKey) });
}

export function preparePickup(adminKey: string, id: number, body: MarkPickupReadyRequest): Promise<PickupResponse> {
  return api(`/admin/orders/${id}/prepare-pickup`, { method: "POST", headers: h(adminKey), body });
}

export function completePickup(adminKey: string, id: number): Promise<PickupResponse> {
  return api(`/admin/orders/${id}/complete-pickup`, { method: "POST", headers: h(adminKey) });
}

export function expirePickups(adminKey: string): Promise<BatchResponse> {
  return api("/admin/orders/expire-pickups", { method: "POST", headers: h(adminKey) });
}
