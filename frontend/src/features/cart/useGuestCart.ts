import { useCallback, useRef, useState } from "react";

const STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";
export const MAX_CART_ITEM_QUANTITY = 99;

export interface GuestCartItem {
  productId: number;
  qty: number;
}

export interface GuestCartMergeRequest {
  userId: number;
  idempotencyKey: string;
  items: GuestCartItem[];
}

export class CartQuantityError extends Error {
  constructor() {
    super(`상품별 장바구니 수량은 1개 이상 ${MAX_CART_ITEM_QUANTITY}개 이하여야 합니다.`);
    this.name = "CartQuantityError";
  }
}

function requireCartQuantity(qty: number) {
  if (!Number.isSafeInteger(qty) || qty < 1 || qty > MAX_CART_ITEM_QUANTITY) {
    throw new CartQuantityError();
  }
}

function isGuestCartItem(value: unknown): value is GuestCartItem {
  if (typeof value !== "object" || value === null) return false;
  const item = value as Partial<GuestCartItem>;
  return typeof item.productId === "number"
    && Number.isSafeInteger(item.productId)
    && item.productId > 0
    && typeof item.qty === "number"
    && Number.isSafeInteger(item.qty)
    && item.qty > 0
    && item.qty <= MAX_CART_ITEM_QUANTITY;
}

function isGuestCartMergeRequest(value: unknown): value is GuestCartMergeRequest {
  if (typeof value !== "object" || value === null) return false;
  const request = value as Partial<GuestCartMergeRequest>;
  if (
    typeof request.userId !== "number"
    || !Number.isSafeInteger(request.userId)
    || request.userId <= 0
    || typeof request.idempotencyKey !== "string"
    || request.idempotencyKey.length === 0
    || !Array.isArray(request.items)
    || request.items.length === 0
    || !request.items.every(isGuestCartItem)
  ) {
    return false;
  }
  return new Set(request.items.map((item) => item.productId)).size === request.items.length;
}

function readGuestCartMergeRequest(): GuestCartMergeRequest | undefined {
  const raw = localStorage.getItem(MERGE_REQUEST_STORAGE_KEY);
  if (!raw) return undefined;
  try {
    const request = JSON.parse(raw) as unknown;
    if (isGuestCartMergeRequest(request)) return request;
  } catch {
    // 손상된 요청은 아래에서 제거하고 현재 장바구니 스냅샷으로 교체한다.
  }
  localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
  return undefined;
}

export function getOrCreateGuestCartMergeRequest(
  userId: number,
  items: GuestCartItem[],
): GuestCartMergeRequest | undefined {
  const saved = readGuestCartMergeRequest();
  if (saved) return saved;

  if (items.length === 0) return undefined;

  const snapshot = items
    .map((item) => ({ ...item }))
    .sort((left, right) => left.productId - right.productId);
  const request = { userId, idempotencyKey: crypto.randomUUID(), items: snapshot };
  localStorage.setItem(MERGE_REQUEST_STORAGE_KEY, JSON.stringify(request));
  return request;
}

export function completeGuestCartMergeRequest(idempotencyKey: string) {
  const raw = localStorage.getItem(MERGE_REQUEST_STORAGE_KEY);
  if (!raw) return;

  try {
    const saved = JSON.parse(raw) as GuestCartMergeRequest;
    if (saved.idempotencyKey === idempotencyKey) {
      localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
    }
  } catch {
    localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
  }
}

export function discardGuestCartMergeRequest(): GuestCartMergeRequest | undefined {
  const request = readGuestCartMergeRequest();
  localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
  return request;
}

function getGuestCartItems(): GuestCartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const saved = JSON.parse(raw) as unknown;
    if (Array.isArray(saved) && saved.every(isGuestCartItem)) return saved;
    localStorage.removeItem(STORAGE_KEY);
    return [];
  } catch {
    localStorage.removeItem(STORAGE_KEY);
    return [];
  }
}

function persist(items: GuestCartItem[]) {
  if (items.length === 0) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export function useGuestCart() {
  const [items, setItems] = useState<GuestCartItem[]>(getGuestCartItems);
  const itemsRef = useRef(items);

  const updateItems = useCallback((update: (current: GuestCartItem[]) => GuestCartItem[]) => {
    const next = update(itemsRef.current);
    persist(next);
    itemsRef.current = next;
    setItems(next);
    return next;
  }, []);

  const addItem = useCallback((productId: number, qty: number) => {
    requireCartQuantity(qty);
    updateItems((prev) => {
      const existing = prev.find((i) => i.productId === productId);
      const nextQty = (existing?.qty ?? 0) + qty;
      requireCartQuantity(nextQty);
      return existing
        ? prev.map((i) =>
          i.productId === productId ? { ...i, qty: nextQty } : i,
        )
        : [...prev, { productId, qty }];
    });
  }, [updateItems]);

  const updateQty = useCallback((productId: number, qty: number) => {
    requireCartQuantity(qty);
    updateItems((prev) =>
      prev.map((i) => (i.productId === productId ? { ...i, qty } : i)),
    );
  }, [updateItems]);

  const removeItem = useCallback((productId: number) => {
    updateItems((prev) => prev.filter((i) => i.productId !== productId));
  }, [updateItems]);

  const consumeMergedItems = useCallback((mergedItems: GuestCartItem[]) => {
    const mergedQuantities = new Map<number, number>();
    for (const item of mergedItems) {
      mergedQuantities.set(
        item.productId,
        (mergedQuantities.get(item.productId) ?? 0) + item.qty,
      );
    }
    return updateItems((current) => current.flatMap((item) => {
      const remainingQty = item.qty - (mergedQuantities.get(item.productId) ?? 0);
      return remainingQty > 0 ? [{ ...item, qty: remainingQty }] : [];
    }));
  }, [updateItems]);

  const itemCount = items.reduce((sum, i) => sum + i.qty, 0);

  return { items, itemCount, addItem, updateQty, removeItem, consumeMergedItems };
}
