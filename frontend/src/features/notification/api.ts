import {
  getMyUnreadNotificationCount,
  listMyNotifications,
  markAllMyNotificationsAsRead,
  markMyNotificationAsRead,
} from "@/generated/api/notification";

export const NOTIFICATION_PAGE_SIZE = 20;

export function fetchNotifications(page: number, unreadOnly = false, signal?: AbortSignal) {
  return listMyNotifications({ page, size: NOTIFICATION_PAGE_SIZE, unreadOnly }, { signal });
}

export function fetchUnreadCount() {
  return getMyUnreadNotificationCount();
}

export function markAsRead(id: number) {
  return markMyNotificationAsRead(id);
}

export function markAllAsRead() {
  return markAllMyNotificationsAsRead();
}
