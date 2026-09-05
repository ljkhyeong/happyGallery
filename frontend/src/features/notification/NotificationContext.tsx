import type { NotificationResponse } from "@/generated/api/notification";
import { formatDateTime } from "@/shared/lib";

export function NotificationContext({ notification }: { notification: NotificationResponse }) {
  if (!notification.contextTitle && !notification.scheduledAt) return null;
  return (
    <div className="small text-body-secondary text-break mt-1">
      {notification.contextTitle && <div>{notification.contextTitle}</div>}
      {notification.scheduledAt && <div>현재 예약일: {formatDateTime(notification.scheduledAt)}</div>}
    </div>
  );
}
