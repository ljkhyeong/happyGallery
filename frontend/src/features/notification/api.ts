import {
  getMyUnreadNotificationCount,
  listMyNotifications,
  markAllMyNotificationsAsRead,
  markMyNotificationAsRead,
} from "@/generated/api/notification";

export function fetchNotifications(page: number, size: number = 20) {
  return listMyNotifications({ page, size });
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
