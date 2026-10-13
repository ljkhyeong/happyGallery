import type { NotificationResponse, NotificationTarget } from "@/generated/api/notification";
import { bookingCreateHref } from "@/features/booking-create/upcomingSlots";
import { productDetailHref } from "@/features/product/navigation";
import { reviewSectionHref } from "@/features/review/reviewSection";

/**
 * 알림을 눌렀을 때 열 화면. 서버가 찾아 준 본인 기록(주문·예약·회차·작품)으로 바로 보내고,
 * 기록이 없거나 상세 화면이 없는 알림은 해당 목록으로 보낸다.
 */
export function notificationTarget(notification: NotificationResponse): string | null {
  // 앱과 화면을 교체하는 동안 이전 서버 응답에는 target이 없을 수 있다.
  const target: Partial<NotificationTarget> = notification.target ?? {};
  const { orderId, bookingId, productId, productVariantId, classId, slotId } = target;
  // 후기 요청은 주문·예약 상세의 후기 칸으로 연다.
  const detail = (path: string) => (notification.eventType === "REVIEW_REQUEST" ? reviewSectionHref(path) : path);
  if (orderId != null) return detail(`/my/orders/${orderId}`);
  if (bookingId != null) return detail(`/my/bookings/${bookingId}`);
  if (classId != null && slotId != null) return bookingCreateHref(classId, slotId);
  if (productId != null) {
    return notification.aggregateType === "PRODUCT_QNA"
      ? `${productDetailHref(productId)}#product-qna`
      : productDetailHref(productId, productVariantId);
  }
  return listTarget(notification);
}

function listTarget(notification: NotificationResponse): string | null {
  switch (notification.aggregateType) {
    case "ORDER":
    case "ORDER_CLAIM":
      return "/my/orders";
    case "BOOKING":
      return "/my/bookings";
    case "RESTOCK_ALERT":
      return "/my/restock-alerts";
    case "VACANCY_ALERT":
      return "/my/vacancy-alerts";
    case "PASS_PURCHASE":
      return "/my/passes";
    case "INQUIRY":
      return "/my/inquiries";
    case "REVIEW":
    case "REVIEW_MODERATION_ACTION":
      return "/my/reviews";
    default:
      return eventTypeTarget(notification.eventType);
  }
}

// 환불처럼 대상 종류만으로 목록을 정할 수 없는 알림은 알림 종류로 고른다.
function eventTypeTarget(eventType: string): string | null {
  if (eventType.startsWith("ORDER_")) return "/my/orders";
  if (eventType.startsWith("BOOKING_") || eventType === "DEPOSIT_REFUNDED"
    || eventType.startsWith("REMINDER_")) return "/my/bookings";
  if (eventType.startsWith("PASS_")) return "/my/passes";
  if (eventType.startsWith("REVIEW_")) return "/my/reviews";
  return null;
}
