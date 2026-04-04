import { adminHeaders as h, api } from "@/shared/api";
import type { CursorPage, AdminOrderResponse, OrderProductionResponse, PickupResponse, BatchResponse, SetExpectedShipDateRequest, MarkPickupReadyRequest, ShippingResponse, OrderHistoryResponse } from "@/shared/types";

export function fetchOrders(
  adminKey: string,
  status?: string,
  cursor?: string,
  size = 20,
): Promise<CursorPage<AdminOrderResponse>> {
  return api<CursorPage<AdminOrderResponse>>("/admin/orders", {
    headers: h(adminKey),
    params: { status, cursor, size: String(size) },
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

export function resumeProduction(adminKey: string, id: number): Promise<OrderProductionResponse> {
  return api(`/admin/orders/${id}/resume-production`, { method: "POST", headers: h(adminKey) });
}

export function preparePickup(adminKey: string, id: number, body: MarkPickupReadyRequest): Promise<PickupResponse> {
  return api(`/admin/orders/${id}/prepare-pickup`, { method: "POST", headers: h(adminKey), body });
}

export function completePickup(adminKey: string, id: number): Promise<PickupResponse> {
  return api(`/admin/orders/${id}/complete-pickup`, { method: "POST", headers: h(adminKey) });
}

export function prepareShipping(adminKey: string, id: number): Promise<ShippingResponse> {
  return api(`/admin/orders/${id}/prepare-shipping`, { method: "POST", headers: h(adminKey) });
}

export function markShipped(adminKey: string, id: number): Promise<ShippingResponse> {
  return api(`/admin/orders/${id}/mark-shipped`, { method: "POST", headers: h(adminKey) });
}

export function markDelivered(adminKey: string, id: number): Promise<ShippingResponse> {
  return api(`/admin/orders/${id}/mark-delivered`, { method: "POST", headers: h(adminKey) });
}

export function fetchOrderHistory(adminKey: string, id: number): Promise<OrderHistoryResponse[]> {
  return api<OrderHistoryResponse[]>(`/admin/orders/${id}/history`, { headers: h(adminKey) });
}

export function expirePickups(adminKey: string): Promise<BatchResponse> {
  return api("/admin/orders/expire-pickups", { method: "POST", headers: h(adminKey) });
}
