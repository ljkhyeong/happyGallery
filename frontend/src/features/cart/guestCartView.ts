import type { CartItemResponse } from "@/generated/api/customerStore";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { GuestCartItem } from "./useGuestCart";
import { productQuantities, productQuantityLimit, productSkuKey } from "@/features/product/purchaseStock";
import { productSelectionView } from "@/features/product/productSelectionView";

export type CartItemIdentifier = number | string;

export type CartItemView = Omit<CartItemResponse, "cartItemId" | "productType"> & {
  cartItemId: CartItemIdentifier;
  productType: CartItemResponse["productType"] | null;
  maxQuantity?: number;
  quantityWarning?: string;
};

export function projectGuestCartItems(
  items: GuestCartItem[],
  products: ProductDetailResponse[],
): CartItemView[] {
  const productsById = new Map(products.map((product) => [product.id, product]));
  const quantitiesBySku = productQuantities(items);

  return items.map((item) => {
    const product = productsById.get(item.productId);
    if (!product) return unavailableGuestItem(item);

    const { variantPriceAdjustment, textOptionPriceAdjustment, options, unitPrice: price,
      configurationValid } = productSelectionView(product, item);
    const skuQuantity = quantitiesBySku.get(productSkuKey(item)) ?? item.qty;
    const limit = productQuantityLimit(product, item.productVariantId);
    const available = product.available && configurationValid && skuQuantity <= limit;

    return {
      cartItemId: item.lineKey,
      productId: item.productId,
      productVariantId: item.productVariantId,
      productName: product.name,
      productType: product.type,
      basePrice: product.price,
      variantPriceAdjustment,
      textOptionPriceAdjustment,
      options,
      price,
      qty: item.qty,
      subtotal: price * item.qty,
      available,
      maxQuantity: Math.max(0, limit - skuQuantity + item.qty),
      quantityWarning: product.available && configurationValid && skuQuantity > limit
        ? `같은 상품·옵션 조합은 합계 ${limit}개까지 주문할 수 있습니다. 수량을 줄여 주세요.`
        : undefined,
      specification: product.specification,
      careInstructions: product.careInstructions,
      productionLeadDays: product.productionLeadDays,
    };
  });
}

function unavailableGuestItem(item: GuestCartItem): CartItemView {
  return {
    cartItemId: item.lineKey,
    productId: item.productId,
    productVariantId: item.productVariantId,
    productName: `상품 #${item.productId}`,
    productType: null,
    basePrice: 0,
    variantPriceAdjustment: 0,
    textOptionPriceAdjustment: 0,
    options: [],
    price: 0,
    qty: item.qty,
    subtotal: 0,
    available: false,
    maxQuantity: 0,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
  };
}
