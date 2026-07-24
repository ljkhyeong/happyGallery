import {
  completeOrderClaimExchange as completeOrderClaimExchangeRequest,
  listAdminOrderClaims,
  resolveOrderClaim as resolveOrderClaimRequest,
} from "@/generated/api/orderClaim";
import type {
  CompleteOrderExchangeRequest,
  ListAdminOrderClaimsStatus,
  ResolveOrderClaimRequest,
} from "@/generated/api/orderClaim";
import { adminHeaders } from "@/shared/api";

export function fetchAdminOrderClaims(
  adminKey: string,
  status?: ListAdminOrderClaimsStatus,
) {
  return listAdminOrderClaims({ status, size: 50 }, {
    headers: adminHeaders(adminKey),
  });
}

export function resolveOrderClaim(
  adminKey: string,
  claimId: number,
  body: ResolveOrderClaimRequest,
) {
  return resolveOrderClaimRequest(claimId, body, {
    headers: adminHeaders(adminKey),
  });
}

export function completeOrderClaimExchange(
  adminKey: string,
  claimId: number,
  body: CompleteOrderExchangeRequest,
) {
  return completeOrderClaimExchangeRequest(claimId, body, {
    headers: adminHeaders(adminKey),
  });
}
