import {
  approve,
  cancelForDelayRejection as cancelDelayedOrder,
  completeProduction as completeOrderProduction,
  confirmPickup,
  expirePickups as expireAdminPickups,
  getFulfillment,
  getOrderHistory,
  ListOrdersStatus,
  listOrders,
  markDelivered as markAdminOrderDelivered,
  markPickupReady,
  markShipped as markAdminOrderShipped,
  prepareShipping as prepareAdminOrderShipping,
  proposeDelay as proposeOrderDelay,
  reject,
  resumeOrderAfterDelay,
  setExpectedShipDate as updateExpectedShipDate,
  type AdminOrderFulfillmentResponse,
  type AdminOrderHistoryResponse,
  type AdminOrderPageResponse,
  type BatchResponse,
  type MarkPickupReadyRequest,
  type MarkShippedRequest,
  type OrderDelayCancellationResponse,
  type OrderProductionResponse,
  type OrderRejectResponse,
  type PickupResponse,
  type SetExpectedShipDateRequest,
  type ShippingResponse,
} from "@/generated/api/adminOrder";
import { adminHeaders } from "@/shared/api";

export function fetchOrders(
  adminKey: string,
  status?: string,
  cursor?: string,
  size = 20,
): Promise<AdminOrderPageResponse> {
  return listOrders(
    { status: orderStatus(status), cursor, size },
    { headers: adminHeaders(adminKey) },
  );
}

function orderStatus(value: string | undefined): ListOrdersStatus | undefined {
  if (value === undefined) {
    return undefined;
  }
  const matched = Object.values(ListOrdersStatus).find((candidate) => candidate === value);
  if (matched === undefined) {
    throw new Error("지원하지 않는 주문 상태입니다.");
  }
  return matched;
}

export function fetchOrderFulfillment(
  adminKey: string,
  id: number,
): Promise<AdminOrderFulfillmentResponse> {
  return getFulfillment(id, { headers: adminHeaders(adminKey) });
}

export function approveOrder(adminKey: string, id: number): Promise<void> {
  return approve(id, { headers: adminHeaders(adminKey) });
}

export function rejectOrder(adminKey: string, id: number): Promise<OrderRejectResponse> {
  return reject(id, { headers: adminHeaders(adminKey) });
}

export function completeProduction(
  adminKey: string,
  id: number,
): Promise<OrderProductionResponse> {
  return completeOrderProduction(id, { headers: adminHeaders(adminKey) });
}

export function setExpectedShipDate(
  adminKey: string,
  id: number,
  body: SetExpectedShipDateRequest,
): Promise<OrderProductionResponse> {
  return updateExpectedShipDate(id, body, { headers: adminHeaders(adminKey) });
}

export function proposeDelay(adminKey: string, id: number): Promise<OrderProductionResponse> {
  return proposeOrderDelay(id, { headers: adminHeaders(adminKey) });
}

export function cancelForDelayRejection(
  adminKey: string,
  id: number,
): Promise<OrderDelayCancellationResponse> {
  return cancelDelayedOrder(id, { headers: adminHeaders(adminKey) });
}

export function resumeAfterDelay(
  adminKey: string,
  id: number,
): Promise<OrderProductionResponse> {
  return resumeOrderAfterDelay(id, { headers: adminHeaders(adminKey) });
}

export function preparePickup(
  adminKey: string,
  id: number,
  body: MarkPickupReadyRequest,
): Promise<PickupResponse> {
  return markPickupReady(id, body, { headers: adminHeaders(adminKey) });
}

export function completePickup(adminKey: string, id: number): Promise<PickupResponse> {
  return confirmPickup(id, { headers: adminHeaders(adminKey) });
}

export function prepareShipping(adminKey: string, id: number): Promise<ShippingResponse> {
  return prepareAdminOrderShipping(id, { headers: adminHeaders(adminKey) });
}

export function markShipped(
  adminKey: string,
  id: number,
  body: MarkShippedRequest,
): Promise<ShippingResponse> {
  return markAdminOrderShipped(id, body, { headers: adminHeaders(adminKey) });
}

export function markDelivered(adminKey: string, id: number): Promise<ShippingResponse> {
  return markAdminOrderDelivered(id, { headers: adminHeaders(adminKey) });
}

export function fetchOrderHistory(
  adminKey: string,
  id: number,
): Promise<AdminOrderHistoryResponse[]> {
  return getOrderHistory(id, { headers: adminHeaders(adminKey) });
}

export function expirePickups(adminKey: string): Promise<BatchResponse> {
  return expireAdminPickups({ headers: adminHeaders(adminKey) });
}
