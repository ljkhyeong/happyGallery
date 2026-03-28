/** 상품 타입 라벨 */
export const PRODUCT_TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "예약 제작",
};

/** 상품 타입별 이행 안내 문구 */
export const PRODUCT_FULFILLMENT_LABEL: Record<string, string> = {
  READY_STOCK: "배송 상품 - 승인 후 출고됩니다.",
  MADE_TO_ORDER: "예약 제작 - 승인 후 제작이 시작됩니다.",
};

/** 주문 이행 유형 라벨 */
export const FULFILLMENT_TYPE_LABEL: Record<string, string> = {
  SHIPPING: "배송",
  PICKUP: "픽업",
};

/** 상품 정렬 라벨 */
export const PRODUCT_SORT_LABEL: Record<string, string> = {
  newest: "최신순",
  price_asc: "가격 낮은순",
  price_desc: "가격 높은순",
};

/** 알림 이벤트 타입 라벨 */
export const NOTIFICATION_EVENT_LABEL: Record<string, string> = {
  BOOKING_CONFIRMED: "예약 확정",
  BOOKING_RESCHEDULED: "예약 변경",
  BOOKING_CANCELED: "예약 취소",
  DEPOSIT_REFUNDED: "예약금 환불",
  ORDER_PAID: "주문 결제 완료",
  ORDER_REFUNDED: "주문 환불",
  REMINDER_D1: "내일 예약 알림",
  REMINDER_SAME_DAY: "오늘 예약 알림",
  PASS_EXPIRY_SOON: "8회권 만료 임박",
  PICKUP_DEADLINE_REMINDER: "픽업 마감 알림",
};
