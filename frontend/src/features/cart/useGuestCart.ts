import { useCallback, useRef, useState } from "react";

const STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";

export interface GuestCartItem {
  productId: number;
  qty: number;
}

interface GuestCartMergeRequest {
  userId: number;
  idempotencyKey: string;
  items: GuestCartItem[];
}

function isGuestCartItem(value: unknown): value is GuestCartItem {
  if (typeof value !== "object" || value === null) return false;
  const item = value as Partial<GuestCartItem>;
  return typeof item.productId === "number"
    && Number.isSafeInteger(item.productId)
    && item.productId > 0
    && typeof item.qty === "number"
    && Number.isSafeInteger(item.qty)
    && item.qty > 0;
}

export function getOrCreateGuestCartMergeRequest(
  userId: number,
  items: GuestCartItem[],
): GuestCartMergeRequest | null | undefined {
  const raw = localStorage.getItem(MERGE_REQUEST_STORAGE_KEY);
  if (raw) {
    try {
      const saved = JSON.parse(raw) as GuestCartMergeRequest;
      if (
        typeof saved.userId === "number"
        && typeof saved.idempotencyKey === "string"
        && Array.isArray(saved.items)
        && saved.items.length > 0
        && saved.items.every(isGuestCartItem)
      ) {
        return saved.userId === userId ? saved : null;
      }
    } catch {
      // 손상된 요청은 현재 장바구니 스냅샷으로 교체한다.
    }
  }

  if (items.length === 0) return undefined;

  const snapshot = [...items].sort((left, right) => left.productId - right.productId);
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

function getGuestCartItems(): GuestCartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const saved = JSON.parse(raw) as unknown;
    return Array.isArray(saved) && saved.every(isGuestCartItem) ? saved : [];
  } catch {
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
    updateItems((prev) => {
      const existing = prev.find((i) => i.productId === productId);
      return existing
        ? prev.map((i) =>
          i.productId === productId ? { ...i, qty: i.qty + qty } : i,
        )
        : [...prev, { productId, qty }];
    });
  }, [updateItems]);

  const updateQty = useCallback((productId: number, qty: number) => {
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
