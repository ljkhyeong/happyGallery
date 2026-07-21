import {
  createContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import type { CartItemResponse } from "@/shared/types/cart";
import { useToast } from "@/shared/ui/ToastContainer";
import {
  addToCart,
  fetchCart,
  mergeGuestCart,
  removeCartItem,
  updateCartItemQty,
} from "./api";
import {
  completeGuestCartMergeRequest,
  getOrCreateGuestCartMergeRequest,
  useGuestCart,
} from "./useGuestCart";

const CART_KEY = ["me", "cart"] as const;

type CartItemView = Omit<CartItemResponse, "productType"> & {
  productType: CartItemResponse["productType"] | null;
};

interface CartContextValue {
  items: CartItemView[];
  totalAmount: number;
  itemCount: number;
  isLoading: boolean;
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
    consumeMergedItems,
  } = useGuestCart();
  const toast = useToast();
  const userId = user?.id ?? null;
  const mergedUserId = useRef<number | null>(null);
  const mergeGeneration = useRef(0);
  const [isMerging, setIsMerging] = useState(false);

  const memberQuery = useQuery({
    queryKey: [...CART_KEY, userId],
    queryFn: fetchCart,
    enabled: userId !== null,
  });

  useEffect(() => {
    if (userId === null) {
      mergedUserId.current = null;
      mergeGeneration.current += 1;
      setIsMerging(false);
      return;
    }
    if (mergedUserId.current === userId) return;

    mergedUserId.current = userId;
    const generation = ++mergeGeneration.current;
    const itemsToMerge = [...guestItems];
    setIsMerging(true);
    void (async () => {
      let merged = false;
      try {
        let remainingItems = itemsToMerge;
        while (true) {
          const mergeRequest = getOrCreateGuestCartMergeRequest(userId, remainingItems);
          if (mergeRequest === undefined) break;
          if (mergeRequest === null) {
            mergedUserId.current = null;
            if (generation === mergeGeneration.current) {
              toast.show(
                "이전 계정의 장바구니 병합을 먼저 확인해 주세요.",
                "warning",
              );
            }
            return;
          }

          await mergeGuestCart(mergeRequest.idempotencyKey, mergeRequest.items);
          merged = true;
          remainingItems = consumeMergedItems(mergeRequest.items);
          completeGuestCartMergeRequest(mergeRequest.idempotencyKey);

          if (generation !== mergeGeneration.current) return;
        }
      } catch {
        if (generation === mergeGeneration.current) {
          toast.show("장바구니를 합치지 못했습니다. 새로고침하면 다시 시도합니다.", "warning");
        }
      } finally {
        if (merged) {
          await queryClient.invalidateQueries({ queryKey: [...CART_KEY, userId] });
        }
        if (generation === mergeGeneration.current) {
          setIsMerging(false);
        }
      }
    })();
  }, [consumeMergedItems, guestItems, queryClient, toast, userId]);

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

  let value: CartContextValue;
  if (userId !== null) {
    const items = memberQuery.data?.items ?? [];
    value = {
      items,
      totalAmount: memberQuery.data?.totalAmount ?? 0,
      itemCount: items.reduce((sum, item) => sum + item.qty, 0),
      isLoading: memberQuery.isLoading || isMerging,
      addItem: (productId, qty) => addMutation.mutateAsync({ productId, qty }),
      updateQty: (productId, qty) => updateMutation.mutateAsync({ productId, qty }),
      removeItem: (productId) => removeMutation.mutateAsync(productId),
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
      })),
      totalAmount: 0,
      itemCount: guestItemCount,
      isLoading: false,
      addItem: async (productId, qty) => addGuestItem(productId, qty),
      updateQty: async (productId, qty) => updateGuestQty(productId, qty),
      removeItem: async (productId) => removeGuestItem(productId),
    };
  }

  return <CartContext value={value}>{children}</CartContext>;
}
