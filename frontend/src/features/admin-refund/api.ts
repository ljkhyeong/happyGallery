import {
  getRefund,
  listFailed,
  retry,
  type FailedRefundPageResponse,
  type RefundStatusResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export function fetchFailedRefunds(
  adminKey: string,
  cursor?: string,
): Promise<FailedRefundPageResponse> {
  return listFailed(
    { cursor, size: 20 },
    { headers: adminHeaders(adminKey) },
  );
}

export function fetchRefund(
  adminKey: string,
  refundId: number,
): Promise<RefundStatusResponse> {
  return getRefund(refundId, { headers: adminHeaders(adminKey) });
}

export function retryRefund(
  adminKey: string,
  refundId: number,
): Promise<RefundStatusResponse> {
  return retry(refundId, { headers: adminHeaders(adminKey) });
}
