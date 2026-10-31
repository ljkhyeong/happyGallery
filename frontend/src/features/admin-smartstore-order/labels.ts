import type { SmartStoreChannelOrderResponseAttentionReason } from "@/generated/api/adminOrder";

export const ATTENTION_LABELS: Record<
  Exclude<SmartStoreChannelOrderResponseAttentionReason, null>,
  string
> = {
  MAPPING_REQUIRED: "상품·옵션 연결 필요",
  STOCK_SHORTAGE: "해피갤러리 재고 부족",
  RETURN_REVIEW: "반품 검수 필요",
  STATUS_REVIEW: "새 주문 상태 확인 필요",
};

export const STATUS_LABELS: Record<string, string> = {
  PAYMENT_WAITING: "결제 대기",
  PAYED: "결제 완료",
  DELIVERING: "배송 중",
  DELIVERED: "배송 완료",
  PURCHASE_DECIDED: "구매 확정",
  EXCHANGED: "교환 완료",
  CANCELED: "취소",
  RETURNED: "반품 완료",
  CANCELED_BY_NOPAYMENT: "미결제 취소",
};

export const ACTION_LABELS: Record<string, string> = {
  INVENTORY_RESOLVED: "재고 반영 방법 지정",
  ORDER_CONFIRMED: "발주 확인",
  ORDER_DISPATCHED: "발송 처리",
  ORDER_DELAYED: "발송 지연",
  CANCEL_APPROVED: "취소 승인",
  RETURN_APPROVED: "반품 승인",
  RETURN_REJECTED: "반품 거부",
  RETURN_HELD: "반품 보류",
  RETURN_HOLD_RELEASED: "반품 보류 해제",
  RETURN_REQUESTED: "판매자 반품 요청",
  EXCHANGE_DISPATCHED: "교환품 발송",
  EXCHANGE_COLLECTION_COMPLETED: "교환품 수거 완료",
  EXCHANGE_REJECTED: "교환 거부",
  EXCHANGE_HELD: "교환 보류",
  EXCHANGE_HOLD_RELEASED: "교환 보류 해제",
  CANCEL_REQUESTED: "판매자 취소 요청",
};

export const ACTION_STATUS_LABELS: Record<string, string> = {
  REQUESTED: "요청 접수",
  SUCCEEDED: "성공",
  REJECTED: "거절",
  NOT_SENT: "요청하지 않음",
  RESULT_UNKNOWN: "결과 확인 필요",
};

export const DELIVERY_METHOD_LABELS = {
  DELIVERY: "택배",
  DIRECT_DELIVERY: "직접 배송",
  VISIT_RECEIPT: "방문 수령",
  QUICK_SVC: "퀵서비스",
};
