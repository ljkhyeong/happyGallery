import {
  approveSmartStoreCancelClaim,
  approveSmartStoreReturnClaim,
  confirmSmartStoreChannelOrder,
  delaySmartStoreChannelOrder,
  dispatchSmartStoreChannelOrder,
  dispatchSmartStoreExchangeClaim,
  getSmartStoreChannelOrder,
  listSmartStoreChannelOrders,
  rejectSmartStoreReturnClaim,
  resolveSmartStoreChannelOrderReturn,
  retrySmartStoreChannelOrderInventory,
  type DelaySmartStoreOrderRequest,
  type DispatchSmartStoreExchangeRequest,
  type DispatchSmartStoreOrderRequest,
} from "@/generated/api/adminOrder";
import { adminHeaders } from "@/shared/api";

export function fetchSmartStoreChannelOrders(
  adminKey: string,
  attentionOnly: boolean,
) {
  return listSmartStoreChannelOrders(
    { attentionOnly, limit: 100 },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchSmartStoreChannelOrder(adminKey: string, productOrderId: string) {
  return getSmartStoreChannelOrder(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function confirmSmartStoreOrder(adminKey: string, productOrderId: string) {
  return confirmSmartStoreChannelOrder(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function dispatchSmartStoreOrder(
  adminKey: string,
  productOrderId: string,
  request: DispatchSmartStoreOrderRequest,
) {
  return dispatchSmartStoreChannelOrder(productOrderId, request, {
    headers: adminHeaders(adminKey),
  });
}

export function delaySmartStoreOrder(
  adminKey: string,
  productOrderId: string,
  request: DelaySmartStoreOrderRequest,
) {
  return delaySmartStoreChannelOrder(productOrderId, request, {
    headers: adminHeaders(adminKey),
  });
}

export function approveSmartStoreCancel(adminKey: string, productOrderId: string) {
  return approveSmartStoreCancelClaim(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function approveSmartStoreReturn(adminKey: string, productOrderId: string) {
  return approveSmartStoreReturnClaim(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function rejectSmartStoreReturn(adminKey: string, productOrderId: string) {
  return rejectSmartStoreReturnClaim(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function dispatchSmartStoreExchange(
  adminKey: string,
  productOrderId: string,
  request: DispatchSmartStoreExchangeRequest,
) {
  return dispatchSmartStoreExchangeClaim(productOrderId, request, {
    headers: adminHeaders(adminKey),
  });
}

export function retrySmartStoreOrderInventory(
  adminKey: string,
  productOrderId: string,
) {
  return retrySmartStoreChannelOrderInventory(productOrderId, {
    headers: adminHeaders(adminKey),
  });
}

export function resolveSmartStoreReturn(
  adminKey: string,
  productOrderId: string,
  restoreStock: boolean,
) {
  return resolveSmartStoreChannelOrderReturn(
    productOrderId,
    { restoreStock },
    { headers: adminHeaders(adminKey) },
  );
}
