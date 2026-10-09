import type { OrderClaimResolution, OrderClaimType } from "./types";

export const ORDER_CLAIM_TYPE_LABELS: Record<OrderClaimType, string> = {
  DAMAGED: "상품 파손·하자",
  WRONG_ITEM: "오배송",
  CHANGE_OF_MIND: "단순 변심",
  OTHER: "기타",
};

export const ORDER_CLAIM_RESOLUTION_LABELS: Record<OrderClaimResolution, string> = {
  REFUND: "환불",
  EXCHANGE: "교환",
};
