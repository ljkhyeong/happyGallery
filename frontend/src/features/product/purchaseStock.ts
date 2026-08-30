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
