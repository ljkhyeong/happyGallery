import type { ProductDetailResponse } from "@/generated/api/product";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

type StockItem = { productId: number; productVariantId: number | null; qty: number };

export function cartSkuKey(item: Pick<StockItem, "productId" | "productVariantId">) {
  return `${item.productId}:${item.productVariantId ?? 0}`;
}

export function cartQuantities(items: readonly StockItem[]) {
  const quantities = new Map<string, number>();
  for (const item of items) {
    const key = cartSkuKey(item);
    quantities.set(key, (quantities.get(key) ?? 0) + item.qty);
  }
  return quantities;
}

export function cartQuantityLimit(product: ProductDetailResponse, variantId: number | null) {
  if (!product.available) return 0;
  if (product.type === "READY_STOCK") return Math.min(MAX_PRODUCT_QUANTITY, product.stockQuantity);
  const variant = product.variants.find((candidate) => candidate.id === variantId && candidate.active);
  return Math.min(MAX_PRODUCT_QUANTITY, variant?.quantity ?? 0);
}
