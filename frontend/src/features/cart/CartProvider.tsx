import {
  createContext,
  type ReactNode,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import type { CartItemResponse } from "@/shared/types/cart";
import type { ProductTextInputRequest } from "@/generated/api/customerStore";
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
  updateQty: (cartItemId: number, qty: number) => Promise<void>;
  removeItem: (cartItemId: number) => Promise<void>;
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
        removeMutation.reset();
        return updateMutation.mutateAsync({ cartItemId, qty });
      },
      removeItem: (cartItemId) => {
        updateMutation.reset();
        return removeMutation.mutateAsync(cartItemId);
      },
    };
  } else {
    value = {
      items: guestItems.map((item) => ({
        cartItemId: item.productId,
        productId: item.productId,
        productVariantId: item.productVariantId,
        productName: "",
        basePrice: 0,
        variantPriceAdjustment: 0,
        textOptionPriceAdjustment: 0,
        options: [],
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
      cartVersion: null,
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
      addItem: async (productId, productVariantId, textInputs, qty) =>
        addGuestItem(productId, productVariantId, textInputs, qty),
      updateQty: async (cartItemId, qty) => updateGuestQty(cartItemId, qty),
      removeItem: async (cartItemId) => removeGuestItem(cartItemId),
    };
  }

  return <CartContext value={value}>{children}</CartContext>;
}
