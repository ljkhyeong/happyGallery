import type { ProductDetailResponse } from "@/generated/api/product";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { selectedProductVariant } from "./productSelectionView";

type StockItem = { productId: number; productVariantId: number | null; qty: number };

export function productSkuKey(item: Pick<StockItem, "productId" | "productVariantId">) {
  return `${item.productId}:${item.productVariantId ?? 0}`;
}

export function productQuantities(items: readonly StockItem[]) {
  const quantities = new Map<string, number>();
  for (const item of items) {
    const key = productSkuKey(item);
    quantities.set(key, (quantities.get(key) ?? 0) + item.qty);
  }
  return quantities;
}

export function productQuantityLimit(product: ProductDetailResponse, variantId: number | null) {
  if (!product.available) return 0;
  if (product.type === "READY_STOCK") return Math.min(MAX_PRODUCT_QUANTITY, product.stockQuantity);
  const variant = variantId === null ? undefined : selectedProductVariant(product, variantId);
  return Math.min(MAX_PRODUCT_QUANTITY, variant?.active ? variant.quantity : 0);
}

/** 같은 작품·옵션 조합 한도에서 이미 담긴 합계를 뺀, 더 담을 수 있는 수량 */
export function remainingAddableQuantity(limit: number, currentQuantity: number) {
  return Math.max(0, limit - currentQuantity);
}

/** 같은 작품·옵션 조합의 다른 줄 수량을 제외하고 이 줄이 가질 수 있는 최대 수량 */
export function remainingLineQuantity(limit: number, totalQuantity: number, lineQuantity: number) {
  return remainingAddableQuantity(limit, totalQuantity - lineQuantity);
}

/** 주문서·장바구니가 같은 문구로 보이는 합계 한도 초과 안내 */
export function quantityLimitExceededMessage(limit: number) {
  return `같은 작품·옵션 조합은 합계 ${limit}개까지 주문할 수 있습니다. 수량을 줄여 주세요.`;
}
