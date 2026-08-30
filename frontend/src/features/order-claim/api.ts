import {
  listGuestOrderClaims,
  listMyOrderClaims,
  requestGuestOrderClaim as requestGuestOrderClaimRequest,
  requestMyOrderClaim,
} from "@/generated/api/orderClaim";
import type { OrderClaimRequest } from "@/generated/api/orderClaim";

export function fetchMemberOrderClaims(orderId: number) {
  return listMyOrderClaims(orderId);
}

export function requestMemberOrderClaim(
  orderId: number,
  body: OrderClaimRequest,
) {
  return requestMyOrderClaim(orderId, body);
}

export function fetchGuestOrderClaims(
  orderId: number,
  accessToken: string,
) {
  return listGuestOrderClaims(orderId, {
    headers: { "X-Access-Token": accessToken },
  });
}

export function requestGuestOrderClaim(
  orderId: number,
  accessToken: string,
  body: OrderClaimRequest,
) {
  return requestGuestOrderClaimRequest(orderId, body, {
    headers: { "X-Access-Token": accessToken },
  });
}
