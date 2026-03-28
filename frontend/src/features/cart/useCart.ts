import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { fetchCart, addToCart, updateCartItemQty, removeCartItem, checkoutCart } from "./api";
import { useGuestCart, getGuestCartItems, clearGuestCart } from "./useGuestCart";
import { useEffect, useRef } from "react";

const CART_KEY = ["me", "cart"] as const;

export function useCart() {
  const { isAuthenticated } = useCustomerAuth();
  const queryClient = useQueryClient();
  const guestCart = useGuestCart();
  const mergedRef = useRef(false);

  const memberQuery = useQuery({
    queryKey: [...CART_KEY],
    queryFn: fetchCart,
    enabled: isAuthenticated,
  });

  // guest → member 병합: 로그인 직후 한 번만 실행
  useEffect(() => {
    if (!isAuthenticated || mergedRef.current) return;
    const guestItems = getGuestCartItems();
    if (guestItems.length === 0) {
      mergedRef.current = true;
      return;
    }
    mergedRef.current = true;
    (async () => {
      for (const item of guestItems) {
        await addToCart(item.productId, item.qty);
      }
      clearGuestCart();
      queryClient.invalidateQueries({ queryKey: [...CART_KEY] });
    })();
  }, [isAuthenticated, queryClient]);

  const addMutation = useMutation({
    mutationFn: ({ productId, qty }: { productId: number; qty: number }) =>
      addToCart(productId, qty),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...CART_KEY] }),
  });

  const updateMutation = useMutation({
    mutationFn: ({ productId, qty }: { productId: number; qty: number }) =>
      updateCartItemQty(productId, qty),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...CART_KEY] }),
  });

  const removeMutation = useMutation({
    mutationFn: (productId: number) => removeCartItem(productId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...CART_KEY] }),
  });

  const checkoutMutation = useMutation({
    mutationFn: checkoutCart,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [...CART_KEY] }),
  });

  if (isAuthenticated) {
    const items = memberQuery.data?.items ?? [];
    return {
      items,
      totalAmount: memberQuery.data?.totalAmount ?? 0,
      itemCount: items.reduce((sum, i) => sum + i.qty, 0),
      isLoading: memberQuery.isLoading,
      addItem: (productId: number, qty: number) => addMutation.mutateAsync({ productId, qty }),
      updateQty: (productId: number, qty: number) => updateMutation.mutateAsync({ productId, qty }),
      removeItem: (productId: number) => removeMutation.mutateAsync(productId),
      checkout: checkoutMutation,
    };
  }

  return {
    items: guestCart.items.map((i) => ({
      productId: i.productId,
      productName: "",
      price: 0,
      qty: i.qty,
      subtotal: 0,
      available: true,
    })),
    totalAmount: 0,
    itemCount: guestCart.itemCount,
    isLoading: false,
    addItem: async (productId: number, qty: number) => { guestCart.addItem(productId, qty); },
    updateQty: async (productId: number, qty: number) => { guestCart.updateQty(productId, qty); },
    removeItem: async (productId: number) => { guestCart.removeItem(productId); },
    checkout: null,
  };
}
