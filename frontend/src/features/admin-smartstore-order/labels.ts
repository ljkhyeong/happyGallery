export const ACTION_LABELS: Record<string, string> = {
  INVENTORY_RESOLVED: "수동 재고 결정",
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
