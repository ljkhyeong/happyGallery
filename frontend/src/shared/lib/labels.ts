/** 상품 타입 라벨 */
export const PRODUCT_TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "주문제작",
};

/** 상품 타입별 이행 안내 문구 */
export const PRODUCT_FULFILLMENT_LABEL: Record<string, string> = {
  READY_STOCK: "배송 상품 - 승인 후 출고됩니다.",
  MADE_TO_ORDER: "고정 사양 주문제작 - 결제 완료 후 안내된 기간에 맞춰 제작합니다.",
};

/** 주문 이행 유형 라벨 */
export const FULFILLMENT_TYPE_LABEL: Record<string, string> = {
  SHIPPING: "배송",
  PICKUP: "픽업",
};

/** 예약 잔금 상태 라벨 */
export const BOOKING_BALANCE_STATUS_LABEL: Record<string, string> = {
  PAID: "결제 완료",
  UNPAID: "현장 결제 예정",
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
  ORDER_APPROVED: "주문 승인",
  ORDER_PICKUP_READY: "픽업 준비 완료",
  ORDER_SHIPPED: "배송 시작",
  ORDER_DELAY_REQUESTED: "주문 처리 지연 동의 요청",
  ORDER_REFUNDED: "주문 환불",
  ORDER_CLAIM_RESOLVED: "주문 클레임 처리 결과",
  ORDER_EXCHANGE_COMPLETED: "주문 교환 완료",
  PASS_PURCHASED: "8회권 결제 완료",
  PASS_REFUNDED: "8회권 환불 완료",
  INQUIRY_ANSWERED: "1:1 문의 답변",
  PRODUCT_QNA_ANSWERED: "상품 Q&A 답변",
  REMINDER_D1: "내일 예약 알림",
  REMINDER_SAME_DAY: "오늘 예약 알림",
  PASS_EXPIRY_SOON: "8회권 만료 임박",
  PICKUP_DEADLINE_REMINDER: "픽업 마감 알림",
};
