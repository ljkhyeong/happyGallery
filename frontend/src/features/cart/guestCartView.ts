import type {
  CartItemResponse,
  ProductOptionSnapshotResponse,
} from "@/generated/api/customerStore";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { GuestCartItem } from "./useGuestCart";
import { cartQuantities, cartQuantityLimit, cartSkuKey } from "./cartStock";

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
  const quantitiesBySku = cartQuantities(items);

  return items.map((item) => {
    const product = productsById.get(item.productId);
    if (!product) return unavailableGuestItem(item);

    const variant = item.productVariantId === null
      ? null
      : product.variants.find((candidate) => candidate.id === item.productVariantId) ?? null;
    const variantPriceAdjustment = variant?.priceAdjustment ?? 0;
    const options = guestOptions(item, product, variant?.selections ?? []);
    const textOptionPriceAdjustment = options
      .filter((option) => option.type === "TEXT")
      .reduce((sum, option) => sum + option.priceAdjustment, 0);
    const price = product.price + variantPriceAdjustment + textOptionPriceAdjustment;
    const skuQuantity = quantitiesBySku.get(cartSkuKey(item)) ?? item.qty;
    const limit = cartQuantityLimit(product, item.productVariantId);
    const available = product.available && skuQuantity <= limit;

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
      quantityWarning: product.available && skuQuantity > limit
        ? `같은 상품·옵션 조합은 합계 ${limit}개까지 주문할 수 있습니다. 수량을 줄여 주세요.`
        : undefined,
      specification: product.specification,
      careInstructions: product.careInstructions,
      productionLeadDays: product.productionLeadDays,
    };
  });
}

function guestOptions(
  item: GuestCartItem,
  product: ProductDetailResponse,
  selections: Array<{ groupKey: string; valueKey: string }>,
): ProductOptionSnapshotResponse[] {
  const selectedOptions = selections.flatMap((selection) => {
    const group = product.optionGroups.find((candidate) =>
      candidate.type === "SELECT" && candidate.key === selection.groupKey,
    );
    const value = group?.values.find((candidate) => candidate.key === selection.valueKey);
    if (!group || !value) return [];
    return [{
      type: "SELECT" as const,
      groupName: group.name,
      value: value.name,
      priceAdjustment: 0,
      sortOrder: group.sortOrder,
    }];
  });
  const textOptions = item.textInputs.flatMap((input) => {
    const value = input.value?.trim() ?? "";
    const group = product.optionGroups.find((candidate) =>
      candidate.type === "TEXT" && candidate.key === input.groupKey,
    );
    if (!group || value.length === 0) return [];
    return [{
      type: "TEXT" as const,
      groupName: group.name,
      value,
      priceAdjustment: group.inputPriceAdjustment ?? 0,
      sortOrder: group.sortOrder,
    }];
  });
  return [...selectedOptions, ...textOptions]
    .sort((left, right) => left.sortOrder - right.sortOrder);
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
