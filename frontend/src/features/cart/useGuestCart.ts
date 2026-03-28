import { useState, useCallback, useEffect } from "react";

const STORAGE_KEY = "hg_guest_cart";

export interface GuestCartItem {
  productId: number;
  qty: number;
}

function load(): GuestCartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function persist(items: GuestCartItem[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export function clearGuestCart() {
  localStorage.removeItem(STORAGE_KEY);
}

export function getGuestCartItems(): GuestCartItem[] {
  return load();
}

export function useGuestCart() {
  const [items, setItems] = useState<GuestCartItem[]>(load);

  useEffect(() => {
    persist(items);
  }, [items]);

  const addItem = useCallback((productId: number, qty: number) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.productId === productId);
      if (existing) {
        return prev.map((i) =>
          i.productId === productId ? { ...i, qty: i.qty + qty } : i,
        );
      }
      return [...prev, { productId, qty }];
    });
  }, []);

  const updateQty = useCallback((productId: number, qty: number) => {
    setItems((prev) =>
      prev.map((i) => (i.productId === productId ? { ...i, qty } : i)),
    );
  }, []);

  const removeItem = useCallback((productId: number) => {
    setItems((prev) => prev.filter((i) => i.productId !== productId));
  }, []);

  const clear = useCallback(() => {
    setItems([]);
  }, []);

  const itemCount = items.reduce((sum, i) => sum + i.qty, 0);

  return { items, itemCount, addItem, updateQty, removeItem, clear };
}
