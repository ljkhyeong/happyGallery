import { useCallback, useRef, useState } from "react";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";

const STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";

export interface GuestCartItem {
  productId: number;
  qty: number;
  lineageId: string;
}

export interface GuestCartMergeRequest {
  userId: number;
  idempotencyKey: string;
  items: GuestCartItem[];
}

export class CartQuantityError extends Error {
  constructor() {
    super(`상품별 장바구니 수량은 1개 이상 ${MAX_PRODUCT_QUANTITY}개 이하여야 합니다.`);
    this.name = "CartQuantityError";
  }
}

function requireCartQuantity(qty: number) {
  if (!Number.isSafeInteger(qty) || qty < 1 || qty > MAX_PRODUCT_QUANTITY) {
    throw new CartQuantityError();
  }
}

function normalizeGuestCartItem(value: unknown): GuestCartItem | undefined {
  if (typeof value !== "object" || value === null) return undefined;
  const item = value as Partial<GuestCartItem>;
  if (
    typeof item.productId !== "number"
    || !Number.isSafeInteger(item.productId)
    || item.productId <= 0
    || typeof item.qty !== "number"
    || !Number.isSafeInteger(item.qty)
    || item.qty <= 0
    || item.qty > MAX_PRODUCT_QUANTITY
    || (item.lineageId !== undefined
      && (typeof item.lineageId !== "string" || item.lineageId.length === 0))
  ) {
    return undefined;
  }
  return {
    productId: item.productId,
    qty: item.qty,
    lineageId: item.lineageId ?? `legacy:${item.productId}`,
  };
}

function normalizeGuestCartItems(value: unknown): GuestCartItem[] | undefined {
  if (!Array.isArray(value)) return undefined;

  const items: GuestCartItem[] = [];
  for (const valueItem of value) {
    const item = normalizeGuestCartItem(valueItem);
    if (!item) return undefined;
    items.push(item);
  }
  return new Set(items.map((item) => item.productId)).size === items.length
    ? items
    : undefined;
}

function normalizeGuestCartMergeRequest(
  value: unknown,
): GuestCartMergeRequest | undefined {
  if (typeof value !== "object" || value === null) return undefined;
  const request = value as Partial<GuestCartMergeRequest>;
  const items = normalizeGuestCartItems(request.items);
  if (
    typeof request.userId !== "number"
    || !Number.isSafeInteger(request.userId)
    || request.userId <= 0
    || typeof request.idempotencyKey !== "string"
    || request.idempotencyKey.length === 0
    || !items
    || items.length === 0
  ) {
    return undefined;
  }
  return {
    userId: request.userId,
    idempotencyKey: request.idempotencyKey,
    items,
  };
}

function readGuestCartMergeRequest(): GuestCartMergeRequest | undefined {
  const raw = localStorage.getItem(MERGE_REQUEST_STORAGE_KEY);
  if (!raw) return undefined;
  try {
    const request = normalizeGuestCartMergeRequest(JSON.parse(raw) as unknown);
    if (request) return request;
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

export function completeGuestCartMergeRequest(
  idempotencyKey: string,
): GuestCartMergeRequest | undefined {
  const request = readGuestCartMergeRequest();
  if (request?.idempotencyKey !== idempotencyKey) return undefined;

  localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
  return request;
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
    const saved = normalizeGuestCartItems(JSON.parse(raw) as unknown);
    if (saved) return saved;
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
        : [...prev, { productId, qty, lineageId: crypto.randomUUID() }];
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
    const mergedQuantities = new Map<string, number>();
    for (const item of mergedItems) {
      const lineageKey = `${item.productId}:${item.lineageId}`;
      mergedQuantities.set(
        lineageKey,
        (mergedQuantities.get(lineageKey) ?? 0) + item.qty,
      );
    }
    return updateItems((current) => current.flatMap((item) => {
      const lineageKey = `${item.productId}:${item.lineageId}`;
      const remainingQty = item.qty - (mergedQuantities.get(lineageKey) ?? 0);
      return remainingQty > 0 ? [{ ...item, qty: remainingQty }] : [];
    }));
  }, [updateItems]);

  const itemCount = items.reduce((sum, i) => sum + i.qty, 0);

  return { items, itemCount, addItem, updateQty, removeItem, consumeMergedItems };
}
