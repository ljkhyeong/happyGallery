import { api } from "@/shared/api";
import type { CartResponse } from "@/shared/types/cart";

interface CartMergeItem {
  productId: number;
  qty: number;
}

export function fetchCart() {
  return api<CartResponse>("/me/cart");
}

export function addToCart(productId: number, qty: number) {
  return api<void>("/me/cart/items", {
    method: "POST",
    body: { productId, qty },
  });
}

export function mergeGuestCart(idempotencyKey: string, items: CartMergeItem[]) {
  return api<void>("/me/cart/merge", {
    method: "POST",
    body: { idempotencyKey, items },
  });
}

export function updateCartItemQty(productId: number, qty: number) {
  return api<void>(`/me/cart/items/${productId}`, {
    method: "PUT",
    body: { qty },
  });
}

export function removeCartItem(productId: number) {
  return api<void>(`/me/cart/items/${productId}`, {
    method: "DELETE",
  });
}
