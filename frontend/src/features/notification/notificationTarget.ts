import type { NotificationResponse } from "@/generated/api/notification";

export function notificationTarget(notification: NotificationResponse): string | null {
  const aggregateId = notification.aggregateId;
  switch (notification.aggregateType) {
    case "ORDER":
      return aggregateId ? `/my/orders/${aggregateId}` : "/my/orders";
    case "BOOKING":
      return aggregateId ? `/my/bookings/${aggregateId}` : "/my/bookings";
    case "RESTOCK_ALERT":
      return "/my#my-restock-alerts";
    case "PASS_PURCHASE":
      return "/my/passes";
    case "INQUIRY":
      return "/my/inquiries";
    case "REVIEW":
    case "REVIEW_MODERATION_ACTION":
      return "/my/reviews";
    default:
      return fallbackTarget(notification.eventType);
  }
}

function fallbackTarget(eventType: string): string | null {
  if (eventType.startsWith("ORDER_") || eventType === "ORDER_REFUNDED") return "/my/orders";
  if (eventType.startsWith("BOOKING_") || eventType === "DEPOSIT_REFUNDED"
    || eventType.startsWith("REMINDER_")) return "/my/bookings";
  if (eventType.startsWith("PASS_")) return "/my/passes";
  if (eventType.startsWith("REVIEW_")) return "/my/reviews";
  return null;
}
