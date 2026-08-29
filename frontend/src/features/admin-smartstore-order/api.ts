import {
  listSmartStoreChannelOrders,
  resolveSmartStoreChannelOrderReturn,
  retrySmartStoreChannelOrderInventory,
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
