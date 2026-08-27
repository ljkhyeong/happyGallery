import {
  createContext,
  useMemo,
  type ReactNode,
} from "react";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import type { ProductTextInputRequest } from "@/generated/api/customerStore";
import { fetchProduct } from "@/features/product/api";
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
import {
  projectGuestCartItems,
  type CartItemIdentifier,
  type CartItemView,
} from "./guestCartView";

interface CartContextValue {
  items: CartItemView[];
  totalAmount: number;
  cartVersion: string | null;
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
  addItem: (
    productId: number,
    productVariantId: number | null,
    textInputs: ProductTextInputRequest[],
    qty: number,
  ) => Promise<void>;
  updateQty: (cartItemId: CartItemIdentifier, qty: number) => Promise<void>;
  removeItem: (cartItemId: CartItemIdentifier) => Promise<void>;
}

export const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: ReactNode }) {
  const { user, sessionVersion } = useCustomerAuth();
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
  const guestProductIds = useMemo(
    () => [...new Set(guestItems.map((item) => item.productId))],
    [guestItems],
  );
  const guestProductQueries = useQueries({
    queries: guestProductIds.map((productId) => ({
      queryKey: queryKeys.catalog.productDetail(productId),
      queryFn: () => fetchProduct(productId),
      enabled: userId === null,
      staleTime: PUBLIC_DATA_STALE_TIME,
    })),
  });
  const {
    isMerging,
    issue: guestCartMergeIssue,
    retry: retryGuestCartMerge,
    discard: discardGuestCartMerge,
  } = useGuestCartMerge({
    userId,
    customerSessionVersion: sessionVersion,
    guestItems,
    consumeMergedItemsWhileLocked,
  });

  const memberQuery = useQuery({
    queryKey: [...queryKeys.member.cart, userId],
    queryFn: fetchCart,
    enabled: userId !== null,
  });

  const addMutation = useMutation({
    mutationFn: ({ productId, productVariantId, textInputs, qty }: {
      productId: number;
      productVariantId: number | null;
      textInputs: ProductTextInputRequest[];
      qty: number;
    }) =>
      runForCurrentCustomer(
        () => addToCart(productId, productVariantId, textInputs, qty),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  const updateMutation = useMutation({
    mutationFn: ({ cartItemId, qty }: { cartItemId: number; qty: number }) =>
      runForCurrentCustomer(
        () => updateCartItemQty(cartItemId, qty),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  const removeMutation = useMutation({
    mutationFn: (cartItemId: number) =>
      runForCurrentCustomer(
        () => removeCartItem(cartItemId),
        () => queryClient.invalidateQueries({ queryKey: queryKeys.member.cart }),
      ),
  });

  let value: CartContextValue;
  if (userId !== null) {
    const items = memberQuery.data?.items ?? [];
    const itemMutationError = addMutation.error ?? updateMutation.error ?? removeMutation.error;
    const isItemMutationPending = addMutation.isPending
      || updateMutation.isPending
      || removeMutation.isPending;
    value = {
      items,
      totalAmount: memberQuery.data?.totalAmount ?? 0,
      cartVersion: memberQuery.data?.cartVersion || null,
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
      addItem: (productId, productVariantId, textInputs, qty) => addMutation.mutateAsync({
        productId,
        productVariantId,
        textInputs,
        qty,
      }),
      updateQty: (cartItemId, qty) => {
        if (typeof cartItemId !== "number") {
          throw new Error("회원 장바구니 항목 번호가 올바르지 않습니다.");
        }
        removeMutation.reset();
        return updateMutation.mutateAsync({ cartItemId, qty });
      },
      removeItem: (cartItemId) => {
        if (typeof cartItemId !== "number") {
          throw new Error("회원 장바구니 항목 번호가 올바르지 않습니다.");
        }
        updateMutation.reset();
        return removeMutation.mutateAsync(cartItemId);
      },
    };
  } else {
    const guestItemsView = projectGuestCartItems(
      guestItems,
      guestProductQueries.flatMap((query) => query.data ? [query.data] : []),
    );
    const guestCatalogError = guestProductQueries.find((query) => query.error)?.error ?? null;
    value = {
      items: guestItemsView,
      totalAmount: guestItemsView
        .filter((item) => item.available)
        .reduce((sum, item) => sum + item.subtotal, 0),
      cartVersion: null,
      itemCount: guestItemCount,
      isLoading: guestItems.length > 0
        && guestProductQueries.some((query) => query.isPending),
      error: guestCatalogError,
      isRefetching: guestProductQueries.some((query) => query.isFetching),
      refetch: () => {
        for (const query of guestProductQueries) void query.refetch();
      },
      itemMutationError: null,
      isItemMutationPending: false,
      guestCartMergeIssue: null,
      retryGuestCartMerge,
      discardGuestCartMerge,
      addItem: async (productId, productVariantId, textInputs, qty) =>
        addGuestItem(productId, productVariantId, textInputs, qty),
      updateQty: async (cartItemId, qty) => updateGuestQty(String(cartItemId), qty),
      removeItem: async (cartItemId) => removeGuestItem(String(cartItemId)),
    };
  }

  return <CartContext value={value}>{children}</CartContext>;
}
