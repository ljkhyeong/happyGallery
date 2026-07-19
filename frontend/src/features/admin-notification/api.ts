import { adminHeaders as h, api } from "@/shared/api";
import type { FailedNotificationResponse } from "@/shared/types";

export function fetchFailedNotifications(adminKey: string): Promise<FailedNotificationResponse[]> {
  return api("/admin/notifications/failed", { headers: h(adminKey) });
}

export function retryNotification(
  adminKey: string,
  outboxId: number,
): Promise<FailedNotificationResponse> {
  return api(`/admin/notifications/${outboxId}/retry`, {
    method: "POST",
    headers: h(adminKey),
  });
}
