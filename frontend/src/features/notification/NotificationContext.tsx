import type { NotificationResponse } from "@/generated/api/notification";
import { formatDateTime } from "@/shared/lib";

export function NotificationContext({ notification }: { notification: NotificationResponse }) {
  if (!notification.contextTitle && !notification.scheduledAt) return null;
  // 빈자리 알림의 일시는 신청한 회차, 그 밖에는 조회 시점의 현재 예약일이다.
  const scheduleLabel = notification.aggregateType === "VACANCY_ALERT" ? "회차" : "현재 예약일";
  return (
    <div className="small text-body-secondary text-break mt-1">
      {notification.contextTitle && <div>{notification.contextTitle}</div>}
      {notification.scheduledAt && <div>{scheduleLabel}: {formatDateTime(notification.scheduledAt)}</div>}
    </div>
  );
}
