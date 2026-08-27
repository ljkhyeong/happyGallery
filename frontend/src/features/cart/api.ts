import {
  addMyCartItem,
  getMyCart,
  mergeMyCartItems,
  removeMyCartItem,
  updateMyCartItemQuantity,
  type CartResponse,
  type MergeCartItemRequest,
  type ProductTextInputRequest,
} from "@/generated/api/customerStore";

type CartMergeItem = MergeCartItemRequest;

export function fetchCart(): Promise<CartResponse> {
  return getMyCart();
}

export function addToCart(
  productId: number,
  productVariantId: number | null,
  textInputs: ProductTextInputRequest[],
  qty: number,
) {
  return addMyCartItem({ productId, productVariantId, textInputs, qty });
}

export function mergeGuestCart(
  expectedCustomerId: number,
  idempotencyKey: string,
  items: CartMergeItem[],
) {
  return mergeMyCartItems({ expectedCustomerId, idempotencyKey, items });
}

export function updateCartItemQty(cartItemId: number, qty: number) {
  return updateMyCartItemQuantity(cartItemId, { qty });
}

export function removeCartItem(cartItemId: number) {
  return removeMyCartItem(cartItemId);
}
