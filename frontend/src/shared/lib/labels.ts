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
