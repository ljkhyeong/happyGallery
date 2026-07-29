import {
  cancelGuestOrder as requestGuestOrderCancellation,
  getGuestOrder,
  getOrderPricePolicy,
  respondToGuestOrderDelay as requestGuestOrderDelayResponse,
  type OrderCustomerActionResponse,
  type OrderDelayResponseRequestDecision,
  type OrderDetailResponse,
  type OrderPricePolicyResponse,
} from "@/generated/api/order";
import {
  cancelMyOrder as requestMyOrderCancellation,
  respondToMyOrderDelay as requestMyOrderDelayResponse,
} from "@/generated/api/customerStore";

export function fetchOrderPricePolicy(): Promise<OrderPricePolicyResponse> {
  return getOrderPricePolicy();
}

export function fetchOrder(id: number, token: string): Promise<OrderDetailResponse> {
  return getGuestOrder(id, {
    headers: { "X-Access-Token": token },
  });
}

export function cancelGuestOrder(id: number, token: string): Promise<OrderCustomerActionResponse> {
  return requestGuestOrderCancellation(id, {
    headers: { "X-Access-Token": token },
  });
}

export function respondToGuestOrderDelay(
  id: number,
  token: string,
  decision: OrderDelayResponseRequestDecision,
): Promise<OrderCustomerActionResponse> {
  return requestGuestOrderDelayResponse(id, { decision }, {
    headers: { "X-Access-Token": token },
  });
}

export function cancelMyOrder(id: number): Promise<OrderCustomerActionResponse> {
  return requestMyOrderCancellation(id);
}

export function respondToMyOrderDelay(
  id: number,
  decision: OrderDelayResponseRequestDecision,
): Promise<OrderCustomerActionResponse> {
  return requestMyOrderDelayResponse(id, { decision });
}
