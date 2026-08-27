/** 클래스 카테고리 선택지 */
export const CLASS_CATEGORY_OPTIONS = [
  { code: "PERFUME", label: "향수" },
  { code: "RESIN", label: "레진아트" },
  { code: "WOOD", label: "목공" },
  { code: "KNIT", label: "뜨개" },
  { code: "LEATHER", label: "가죽공예" },
  { code: "UPCYCLING", label: "업사이클링" },
] as const;

export function getClassCategoryLabel(category: string): string {
  const normalizedCategory = category.trim().toUpperCase();
  return CLASS_CATEGORY_OPTIONS.find(({ code }) => code === normalizedCategory)?.label
    ?? category;
}

export function isPerfumeClassCategory(category: string): boolean {
  return category.trim().toUpperCase() === "PERFUME";
}

/** 상품 타입 라벨 */
export const PRODUCT_TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "재고 상품",
  MADE_TO_ORDER: "주문 제작 상품",
};

/** 상품 타입별 배송·제작 안내 문구 */
export const PRODUCT_FULFILLMENT_LABEL: Record<string, string> = {
  READY_STOCK: "재고 상품 - 공방 승인 후 선택한 수령 방법에 맞춰 준비합니다.",
  MADE_TO_ORDER: "주문 제작 상품 - 공방 승인 후 상품에 안내된 기간과 사양에 맞춰 제작합니다.",
};

/** 상품 수령 방법 라벨 */
export const FULFILLMENT_TYPE_LABEL: Record<string, string> = {
  SHIPPING: "택배 배송",
  PICKUP: "매장 수령",
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
  ORDER_PICKUP_READY: "매장 수령 준비 완료",
  ORDER_SHIPPED: "배송 시작",
  ORDER_DELAY_REQUESTED: "주문 지연 일정 확인 요청",
  ORDER_REFUNDED: "주문 환불",
  ORDER_CLAIM_RESOLVED: "환불·교환 요청 처리 결과",
  ORDER_EXCHANGE_COMPLETED: "주문 교환 완료",
  PASS_PURCHASED: "8회권 결제 완료",
  PASS_REFUNDED: "8회권 환불 완료",
  INQUIRY_ANSWERED: "1:1 문의 답변",
  PRODUCT_QNA_ANSWERED: "상품 문의 답변",
  REVIEW_REQUEST: "후기 작성 요청",
  REVIEW_HIDDEN: "후기 비공개 안내",
  REVIEW_REPUBLISHED: "후기 재공개 안내",
  REVIEW_OWNER_REPLIED: "내 후기에 공방 답글",
  REMINDER_D1: "내일 예약 알림",
  REMINDER_SAME_DAY: "오늘 예약 알림",
  PASS_EXPIRY_SOON: "8회권 만료 임박",
  PICKUP_DEADLINE_REMINDER: "매장 수령 마감 알림",
};

export type StatusAudience = "customer" | "admin";

const STATUS_LABEL: Record<string, string> = {
  BOOKED: "예약 완료",
  CANCELED: "예약 취소",
  NO_SHOW: "미참석",
  COMPLETED: "완료",
  REJECTED: "주문 거절",
  CUSTOMER_CANCELED: "고객 취소",
  AUTO_REFUND_TIMEOUT: "자동 환불 처리",
  IN_PRODUCTION: "제작 중",
  DELAY_REJECTED_CANCELED: "지연 거절로 주문 취소",
  PICKED_UP: "수령 완료",
  PICKUP_EXPIRED: "미수령 환불 처리",
  PICKUP_FORFEITED: "미수령 종료(환불 없음)",
  SHIPPING_PREPARING: "배송 준비 중",
  SHIPPED: "배송 중",
  DELIVERED: "배송 완료",
  READY: "결제 준비",
  REFUNDING: "환불 처리 중",
  REFUNDED: "환불 완료",
  EXPIRED: "만료",
  ACTIVE: "사용 가능",
  USED_UP: "모두 사용",
  REFUND_PENDING: "환불 처리 중",
  REFUND_FAILED: "환불 확인 필요",
};

const CUSTOMER_STATUS_LABEL: Record<string, string> = {
  PAID_APPROVAL_PENDING: "공방 승인 대기",
  APPROVED_FULFILLMENT_PENDING: "상품 준비 대기",
  DELAY_CONSENT_PENDING: "지연 안내 확인 필요",
  DELAY_ACCEPTED: "지연 일정 동의",
  PICKUP_READY: "매장에서 수령 가능",
  CONFIRMING: "결제 결과 확인 중",
  RETRYABLE: "결제 결과 다시 확인 필요",
  FAILED: "결제 실패",
  REVIEW_REQUIRED: "결제사 결과 확인 중",
  SUPPORT_REQUIRED: "환불 확인 필요",
};

const ADMIN_STATUS_LABEL: Record<string, string> = {
  PAID_APPROVAL_PENDING: "승인 필요",
  APPROVED_FULFILLMENT_PENDING: "주문 처리 대기",
  DELAY_CONSENT_PENDING: "고객 응답 대기",
  DELAY_ACCEPTED: "고객 지연 동의",
  PICKUP_READY: "고객 수령 대기",
  CONFIRMING: "결제 결과 확인 중",
  RETRYABLE: "결제 결과 재확인 필요",
  FAILED: "결제 실패",
  REVIEW_REQUIRED: "결제사 결과 확인 필요",
  SUPPORT_REQUIRED: "관리자 확인 필요",
};

const UNKNOWN_STATUS_LABEL = "상태 확인 필요";

export function getStatusLabel(
  status: string,
  audience: StatusAudience = "customer",
): string {
  const audienceLabels = audience === "admin" ? ADMIN_STATUS_LABEL : CUSTOMER_STATUS_LABEL;
  return audienceLabels[status] ?? STATUS_LABEL[status] ?? UNKNOWN_STATUS_LABEL;
}
