import { api } from "@/shared/api";
import type { NotificationResponse, UnreadCountResponse } from "@/shared/types/notification";

export function fetchNotifications(page: number, size: number = 20) {
  return api<NotificationResponse[]>("/me/notifications", {
    params: { page, size },
  });
}

export function fetchUnreadCount() {
  return api<UnreadCountResponse>("/me/notifications/unread-count");
}

export function markAsRead(id: number) {
  return api<void>(`/me/notifications/${id}/read`, { method: "PATCH" });
}

export function markAllAsRead() {
  return api<void>("/me/notifications/read-all", { method: "PATCH" });
}
