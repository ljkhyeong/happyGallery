import {
  getMyCart,
  mergeMyCartItems,
  removeMyCartItem,
  updateMyCartItemQuantity,
  type CartResponse,
  type ProductSelectionRequest,
} from "@/generated/api/customerStore";

type CartMergeItem = ProductSelectionRequest;

export function fetchCart(): Promise<CartResponse> {
  return getMyCart();
}

export function mergeCartItems(
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
