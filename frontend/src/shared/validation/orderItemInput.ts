import type { OrderItemInput } from "@/shared/types";
import { MAX_PRODUCT_QUANTITY } from "./productQuantity";

/** 브라우저 저장소에 둔 주문 항목. 서버 계약에서 선택인 옵션 조합·글자 입력도 항상 채워져 있다. */
export type StoredOrderItemInput = Required<OrderItemInput>;

interface Options {
  /** 글자 입력 value가 비어 있는(undefined) 항목도 받는다. 비회원 장바구니 저장값 전용. */
  allowUndefinedText?: boolean;
}

/**
 * 저장소에서 읽은 값이 주문 항목 입력(작품 ID·옵션 조합·수량·글자 입력) 형태인지 검사한다.
 * 비회원 주문 초안과 비회원 장바구니가 같은 규칙을 쓴다.
 */
export function isOrderItemInputLike(
  value: unknown,
  { allowUndefinedText = false }: Options = {},
): value is StoredOrderItemInput {
  if (typeof value !== "object" || value === null) return false;
  const item = value as StoredOrderItemInput;
  return Number.isSafeInteger(item.productId) && item.productId > 0
    && (item.productVariantId === null
      || (Number.isSafeInteger(item.productVariantId) && item.productVariantId > 0))
    && Number.isSafeInteger(item.qty) && item.qty >= 1 && item.qty <= MAX_PRODUCT_QUANTITY
    && Array.isArray(item.textInputs)
    && item.textInputs.every((input) => (
      typeof input === "object" && input !== null
      && typeof input.groupKey === "string"
      && (typeof input.value === "string" || (allowUndefinedText && input.value === undefined))
    ));
}
