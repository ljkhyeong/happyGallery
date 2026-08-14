import {
  addMyCartItem,
  getMyCart,
  mergeMyCartItems,
  removeMyCartItem,
  updateMyCartItemQuantity,
  type CartResponse,
  type MergeCartItemRequest,
} from "@/generated/api/customerStore";

type CartMergeItem = MergeCartItemRequest;

export function fetchCart(): Promise<CartResponse> {
  return getMyCart();
}

export function addToCart(productId: number, qty: number) {
  return addMyCartItem({ productId, qty });
}

export function mergeGuestCart(
  expectedCustomerId: number,
  idempotencyKey: string,
  items: CartMergeItem[],
) {
  return mergeMyCartItems({ expectedCustomerId, idempotencyKey, items });
}

export function updateCartItemQty(productId: number, qty: number) {
  return updateMyCartItemQuantity(productId, { qty });
}

export function removeCartItem(productId: number) {
  return removeMyCartItem(productId);
}
