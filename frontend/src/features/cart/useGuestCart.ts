import { useCallback, useEffect, useState } from "react";
import { MAX_PRODUCT_QUANTITY } from "@/shared/validation/productQuantity";
import { editGuestCartExclusive } from "./guestCartLock";
import type { ProductTextInputRequest } from "@/generated/api/customerStore";

const STORAGE_KEY = "hg_guest_cart";
const MERGE_REQUEST_STORAGE_KEY = "hg_guest_cart_merge_request";

export interface GuestCartItem {
  productId: number;
  productVariantId: number | null;
  textInputs: ProductTextInputRequest[];
  lineKey: string;
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

function guestLineKey(
  productId: number,
  productVariantId: number | null,
  textInputs: ProductTextInputRequest[],
) {
  const inputs = [...textInputs]
    .sort((left, right) => left.groupKey.localeCompare(right.groupKey))
    .map((input) => `${input.groupKey}=${input.value ?? ""}`)
    .join("|");
  return `${productId}:${productVariantId ?? 0}:${inputs}`;
}

function normalizeGuestCartItem(value: unknown): GuestCartItem | undefined {
  if (typeof value !== "object" || value === null) return undefined;
  const item = value as Partial<GuestCartItem>;
  if (
    typeof item.productId !== "number"
    || !Number.isSafeInteger(item.productId)
    || item.productId <= 0
    || (item.productVariantId !== undefined
      && item.productVariantId !== null
      && (!Number.isSafeInteger(item.productVariantId) || item.productVariantId <= 0))
    || (item.textInputs !== undefined && !Array.isArray(item.textInputs))
    || typeof item.qty !== "number"
    || !Number.isSafeInteger(item.qty)
    || item.qty <= 0
    || item.qty > MAX_PRODUCT_QUANTITY
    || (item.lineageId !== undefined
      && (typeof item.lineageId !== "string" || item.lineageId.length === 0))
  ) {
    return undefined;
  }
  const textInputs = (item.textInputs ?? []).filter((input) => (
    typeof input === "object"
    && input !== null
    && typeof input.groupKey === "string"
    && (input.value === undefined || typeof input.value === "string")
  )) as ProductTextInputRequest[];
  if (textInputs.length !== (item.textInputs ?? []).length) return undefined;
  const productVariantId = item.productVariantId ?? null;
  return {
    productId: item.productId,
    productVariantId,
    textInputs,
    lineKey: guestLineKey(item.productId, productVariantId, textInputs),
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
  return new Set(items.map((item) => item.lineKey)).size === items.length
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

function readGuestCartMergeRequestWhileLocked(): GuestCartMergeRequest | undefined {
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

export function getOrCreateGuestCartMergeRequestWhileLocked(
  userId: number,
): GuestCartMergeRequest | undefined {
  const saved = readGuestCartMergeRequestWhileLocked();
  if (saved) return saved;

  const items = readGuestCartItemsWhileLocked();
  if (items.length === 0) return undefined;

  const snapshot = items
    .map((item) => ({ ...item }))
    .sort((left, right) => left.lineKey.localeCompare(right.lineKey));
  const request = { userId, idempotencyKey: crypto.randomUUID(), items: snapshot };
  localStorage.setItem(MERGE_REQUEST_STORAGE_KEY, JSON.stringify(request));
  return request;
}

export function completeGuestCartMergeRequestWhileLocked(
  idempotencyKey: string,
): void {
  const request = readGuestCartMergeRequestWhileLocked();
  if (request?.idempotencyKey !== idempotencyKey) return;

  localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
}

export function discardGuestCartMergeRequestWhileLocked(): GuestCartMergeRequest | undefined {
  const request = readGuestCartMergeRequestWhileLocked();
  localStorage.removeItem(MERGE_REQUEST_STORAGE_KEY);
  return request;
}

export function readGuestCartItems(): GuestCartItem[] {
  if (typeof window === "undefined") return [];

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const saved = normalizeGuestCartItems(JSON.parse(raw) as unknown);
    if (saved) return saved;
  } catch {
    // 손상된 저장값은 잠금을 획득한 변경 경로에서 제거한다.
  }
  return [];
}

function readGuestCartItemsWhileLocked(): GuestCartItem[] {
  const items = readGuestCartItems();
  if (items.length === 0 && localStorage.getItem(STORAGE_KEY) !== null) {
    localStorage.removeItem(STORAGE_KEY);
  }
  return items;
}

function persistGuestCartItemsWhileLocked(items: GuestCartItem[]) {
  if (items.length === 0) {
    localStorage.removeItem(STORAGE_KEY);
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export function useGuestCart() {
  const [items, setItems] = useState<GuestCartItem[]>([]);

  useEffect(() => {
    const synchronizeGuestCart = (event: StorageEvent) => {
      if (
        event.storageArea !== localStorage
        || (event.key !== STORAGE_KEY && event.key !== null)
      ) {
        return;
      }
      setItems(readGuestCartItems());
    };

    setItems(readGuestCartItems());
    window.addEventListener("storage", synchronizeGuestCart);
    return () => window.removeEventListener("storage", synchronizeGuestCart);
  }, []);

  const updateItemsWhileLocked = useCallback(
    (update: (current: GuestCartItem[]) => GuestCartItem[]) => {
      const next = update(readGuestCartItemsWhileLocked());
      persistGuestCartItemsWhileLocked(next);
      setItems(next);
      return next;
    },
    [],
  );

  const updateItems = useCallback(
    (update: (current: GuestCartItem[]) => GuestCartItem[]) =>
      editGuestCartExclusive(() => updateItemsWhileLocked(update)),
    [updateItemsWhileLocked],
  );

  const addItem = useCallback(async (
    productId: number,
    productVariantId: number | null,
    textInputs: ProductTextInputRequest[],
    qty: number,
  ) => {
    requireCartQuantity(qty);
    const lineKey = guestLineKey(productId, productVariantId, textInputs);
    await updateItems((prev) => {
      const existing = prev.find((item) => item.lineKey === lineKey);
      const nextQty = (existing?.qty ?? 0) + qty;
      requireCartQuantity(nextQty);
      return existing
        ? prev.map((i) =>
          i.lineKey === lineKey ? { ...i, qty: nextQty } : i,
        )
        : [...prev, {
          productId,
          productVariantId,
          textInputs,
          lineKey,
          qty,
          lineageId: crypto.randomUUID(),
        }];
    });
  }, [updateItems]);

  const updateQty = useCallback(async (lineKey: string, qty: number) => {
    requireCartQuantity(qty);
    await updateItems((prev) =>
      prev.map((item) => (item.lineKey === lineKey ? { ...item, qty } : item)),
    );
  }, [updateItems]);

  const removeItem = useCallback(async (lineKey: string) => {
    await updateItems((prev) => prev.filter((item) => item.lineKey !== lineKey));
  }, [updateItems]);

  const consumeMergedItemsWhileLocked = useCallback((mergedItems: GuestCartItem[]) => {
    const mergedQuantities = new Map<string, number>();
    for (const item of mergedItems) {
      const lineageKey = `${item.productId}:${item.lineageId}`;
      mergedQuantities.set(
        lineageKey,
        (mergedQuantities.get(lineageKey) ?? 0) + item.qty,
      );
    }
    return updateItemsWhileLocked((current) => current.flatMap((item) => {
      const lineageKey = `${item.productId}:${item.lineageId}`;
      const remainingQty = item.qty - (mergedQuantities.get(lineageKey) ?? 0);
      return remainingQty > 0 ? [{ ...item, qty: remainingQty }] : [];
    }));
  }, [updateItemsWhileLocked]);

  const itemCount = items.reduce((sum, i) => sum + i.qty, 0);

  return {
    items,
    itemCount,
    addItem,
    updateQty,
    removeItem,
    consumeMergedItemsWhileLocked,
  };
}
