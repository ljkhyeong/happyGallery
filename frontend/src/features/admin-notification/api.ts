import {
  listFailedNotifications,
  retryNotification as retryFailedNotification,
  type FailedNotificationResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export function fetchFailedNotifications(
  adminKey: string,
): Promise<FailedNotificationResponse[]> {
  return listFailedNotifications({ headers: adminHeaders(adminKey) });
}

export function retryNotification(
  adminKey: string,
  outboxId: number,
): Promise<FailedNotificationResponse> {
  return retryFailedNotification(outboxId, { headers: adminHeaders(adminKey) });
}
