import {
  useCallback,
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
import { ApiError } from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import {
  addToCart,
  fetchCart,
  mergeGuestCart,
  removeCartItem,
  updateCartItemQty,
} from "./api";
import {
  completeGuestCartMergeRequest,
  discardGuestCartMergeRequest,
  getOrCreateGuestCartMergeRequest,
  useGuestCart,
} from "./useGuestCart";

const CART_KEY = ["me", "cart"] as const;

type CartItemView = Omit<CartItemResponse, "productType"> & {
  productType: CartItemResponse["productType"] | null;
};

export interface GuestCartMergeIssue {
  kind: "ACCOUNT_MISMATCH" | "MERGE_FAILED";
  message: string;
  canRetry: boolean;
}

interface CartContextValue {
  items: CartItemView[];
  totalAmount: number;
  itemCount: number;
  isLoading: boolean;
  guestCartMergeIssue: GuestCartMergeIssue | null;
  retryGuestCartMerge: () => void;
  discardGuestCartMerge: () => void;
  addItem: (productId: number, qty: number) => Promise<void>;
  updateQty: (productId: number, qty: number) => Promise<void>;
  removeItem: (productId: number) => Promise<void>;
}

export const CartContext = createContext<CartContextValue | null>(null);

function mergeFailureMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return getUserMessage(error.code)
      ?? (error.status >= 500
        ? "서버에 일시적인 문제가 발생했습니다."
        : error.message);
  }
  if (error instanceof Error && error.name === "AbortError") {
    return "요청 시간이 초과되었습니다.";
  }
  if (error instanceof TypeError) {
    return "서버에 연결할 수 없습니다.";
  }
  return "알 수 없는 오류가 발생했습니다.";
}

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
  const [mergeAttempt, setMergeAttempt] = useState(0);
  const [isMerging, setIsMerging] = useState(false);
  const [guestCartMergeIssue, setGuestCartMergeIssue] =
    useState<GuestCartMergeIssue | null>(null);

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
      setGuestCartMergeIssue(null);
      return;
    }
    if (mergedUserId.current === userId) return;

    mergedUserId.current = userId;
    const generation = ++mergeGeneration.current;
    const itemsToMerge = [...guestItems];
    setIsMerging(true);
    setGuestCartMergeIssue(null);
    void (async () => {
      let merged = false;
      try {
        let remainingItems = itemsToMerge;
        while (true) {
          const mergeRequest = getOrCreateGuestCartMergeRequest(userId, remainingItems);
          if (mergeRequest === undefined) break;
          if (mergeRequest.userId !== userId) {
            if (generation === mergeGeneration.current) {
              const issue: GuestCartMergeIssue = {
                kind: "ACCOUNT_MISMATCH",
                message: "다른 계정에서 시작한 장바구니 병합이 보류 중입니다. 해당 계정으로 다시 로그인해 재시도하거나 보류 항목을 폐기해 주세요.",
                canRetry: false,
              };
              setGuestCartMergeIssue(issue);
              toast.show(issue.message, "warning");
            }
            return;
          }

          await mergeGuestCart(mergeRequest.idempotencyKey, mergeRequest.items);
          merged = true;
          remainingItems = consumeMergedItems(mergeRequest.items);
          completeGuestCartMergeRequest(mergeRequest.idempotencyKey);

          if (generation !== mergeGeneration.current) return;
          setGuestCartMergeIssue(null);
        }
      } catch (error) {
        if (generation === mergeGeneration.current) {
          const issue: GuestCartMergeIssue = {
            kind: "MERGE_FAILED",
            message: `장바구니를 합치지 못했습니다. ${mergeFailureMessage(error)}`,
            canRetry: true,
          };
          setGuestCartMergeIssue(issue);
          toast.show(issue.message, "warning");
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
  }, [consumeMergedItems, guestItems, mergeAttempt, queryClient, toast, userId]);

  const retryGuestCartMerge = useCallback(() => {
    if (userId === null) return;
    mergedUserId.current = null;
    mergeGeneration.current += 1;
    setGuestCartMergeIssue(null);
    setMergeAttempt((attempt) => attempt + 1);
  }, [userId]);

  const discardGuestCartMerge = useCallback(() => {
    const discarded = discardGuestCartMergeRequest();
    if (discarded) {
      consumeMergedItems(discarded.items);
    }
    mergedUserId.current = null;
    mergeGeneration.current += 1;
    setIsMerging(false);
    setGuestCartMergeIssue(null);
    setMergeAttempt((attempt) => attempt + 1);
  }, [consumeMergedItems]);

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
      guestCartMergeIssue,
      retryGuestCartMerge,
      discardGuestCartMerge,
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
