import {
  createContext,
  type ReactNode,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import type { CartItemResponse } from "@/shared/types/cart";
import {
  addToCart,
  fetchCart,
  removeCartItem,
  updateCartItemQty,
} from "./api";
import { useGuestCart } from "./useGuestCart";
import {
  useGuestCartMerge,
  type GuestCartMergeIssue,
} from "./useGuestCartMerge";

type CartItemView = Omit<CartItemResponse, "productType"> & {
  productType: CartItemResponse["productType"] | null;
};

interface CartContextValue {
  items: CartItemView[];
  totalAmount: number;
  itemCount: number;
  isLoading: boolean;
  error: unknown;
  isRefetching: boolean;
  refetch: () => void;
  itemMutationError: unknown;
  isItemMutationPending: boolean;
  guestCartMergeIssue: GuestCartMergeIssue | null;
  retryGuestCartMerge: () => void;
  discardGuestCartMerge: () => void;
  addItem: (productId: number, qty: number) => Promise<void>;
  updateQty: (productId: number, qty: number) => Promise<void>;
  removeItem: (productId: number) => Promise<void>;
}

export const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { user } = useCustomerAuth();
  const queryClient = useQueryClient();
  const {
    items: guestItems,
    itemCount: guestItemCount,
    addItem: addGuestItem,
    updateQty: updateGuestQty,
    removeItem: removeGuestItem,
    consumeMergedItemsWhileLocked,
  } = useGuestCart();
  const userId = user?.id ?? null;
  const {
    isMerging,
    issue: guestCartMergeIssue,
    retry: retryGuestCartMerge,
    discard: discardGuestCartMerge,
  } = useGuestCartMerge({
    userId,
    guestItems,
    consumeMergedItemsWhileLocked,
  });

  const memberQuery = useQuery({
    queryKey: [...queryKeys.member.cart, userId],
    queryFn: fetchCart,
    enabled: userId !== null,
  });

  const addMutation = useMutation({
    mutationFn: ({ productId, qty }: { productId: number; qty: number }) =>
      runForCurrentCustomer(
        () => addToCart(productId, qty),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  const updateMutation = useMutation({
    mutationFn: ({ productId, qty }: { productId: number; qty: number }) =>
      runForCurrentCustomer(
        () => updateCartItemQty(productId, qty),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  const removeMutation = useMutation({
    mutationFn: (productId: number) =>
      runForCurrentCustomer(
        () => removeCartItem(productId),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  let value: CartContextValue;
  if (userId !== null) {
    const items = memberQuery.data?.items ?? [];
    const itemMutationError = updateMutation.error ?? removeMutation.error;
    const isItemMutationPending = updateMutation.isPending || removeMutation.isPending;
    value = {
      items,
      totalAmount: memberQuery.data?.totalAmount ?? 0,
      itemCount: items.reduce((sum, item) => sum + item.qty, 0),
      isLoading: memberQuery.isLoading || isMerging,
      error: memberQuery.error,
      isRefetching: memberQuery.isFetching,
      refetch: () => { void memberQuery.refetch(); },
      itemMutationError,
      isItemMutationPending,
      guestCartMergeIssue,
      retryGuestCartMerge,
      discardGuestCartMerge,
      addItem: (productId, qty) => addMutation.mutateAsync({ productId, qty }),
      updateQty: (productId, qty) => {
        removeMutation.reset();
        return updateMutation.mutateAsync({ productId, qty });
      },
      removeItem: (productId) => {
        updateMutation.reset();
        return removeMutation.mutateAsync(productId);
      },
    };
  } else {
    value = {
      items: guestItems.map((item) => ({
        productId: item.productId,
        productName: "",
        price: 0,
        qty: item.qty,
        subtotal: 0,
        available: true,
        productType: null,
        specification: null,
        careInstructions: null,
        productionLeadDays: null,
      })),
      totalAmount: 0,
      itemCount: guestItemCount,
      isLoading: false,
      error: null,
      isRefetching: false,
      refetch: () => undefined,
      itemMutationError: null,
      isItemMutationPending: false,
      guestCartMergeIssue: null,
      retryGuestCartMerge,
      discardGuestCartMerge,
      addItem: async (productId, qty) => addGuestItem(productId, qty),
      updateQty: async (productId, qty) => updateGuestQty(productId, qty),
      removeItem: async (productId) => removeGuestItem(productId),
    };
  }

  return <CartContext value={value}>{children}</CartContext>;
}
