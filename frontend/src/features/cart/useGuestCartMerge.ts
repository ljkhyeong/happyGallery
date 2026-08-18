import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  ApiError,
  captureCustomerSession,
  CustomerSessionChangedError,
  currentCustomerSessionUserId,
  queryKeys,
  requireCurrentCustomerSession,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { getUserMessage } from "@/shared/lib";
import { useToast } from "@/shared/ui/ToastContainer";
import { mergeGuestCart } from "./api";
import {
  editGuestCartExclusive,
  GuestCartLockUnavailableError,
  mergeGuestCartExclusive,
} from "./guestCartLock";
import {
  completeGuestCartMergeRequestWhileLocked,
  discardGuestCartMergeRequestWhileLocked,
  getOrCreateGuestCartMergeRequestWhileLocked,
  type GuestCartItem,
} from "./useGuestCart";

export interface GuestCartMergeIssue {
  kind: "ACCOUNT_MISMATCH" | "MERGE_FAILED";
  message: string;
  canRetry: boolean;
}

interface UseGuestCartMergeParams {
  userId: number | null;
  customerSessionVersion: number;
  guestItems: GuestCartItem[];
  consumeMergedItemsWhileLocked: (items: GuestCartItem[]) => GuestCartItem[];
}

function requireMergeCustomerSession(
  snapshot: CustomerSessionSnapshot,
  userId: number,
): void {
  requireCurrentCustomerSession(snapshot);
  if (
    snapshot.boundaryCustomerId !== userId
    || currentCustomerSessionUserId() !== userId
  ) {
    throw new CustomerSessionChangedError();
  }
}

function mergeFailureMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return getUserMessage(error.code)
      ?? (error.status >= 500
        ? "서비스에 일시적인 문제가 발생했습니다."
        : "장바구니를 합칠 수 없습니다. 다시 시도해 주세요.");
  }
  if (error instanceof Error && error.name === "AbortError") {
    return "요청 시간이 초과되었습니다.";
  }
  if (error instanceof TypeError && error.message === "Failed to fetch") {
    return "서비스에 연결할 수 없습니다.";
  }
  if (error instanceof GuestCartLockUnavailableError) {
    return error.message;
  }
  return "잠시 후 다시 시도해 주세요.";
}

export function useGuestCartMerge({
  userId,
  customerSessionVersion,
  guestItems,
  consumeMergedItemsWhileLocked,
}: UseGuestCartMergeParams) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const mergedUserId = useRef<number | null>(null);
  const mergeGeneration = useRef(0);
  const mergeBlocked = useRef(false);
  const [mergeRevision, setMergeRevision] = useState(0);
  const [isMerging, setIsMerging] = useState(false);
  const [issue, setIssue] = useState<GuestCartMergeIssue | null>(null);

  useEffect(() => {
    if (userId === null) {
      mergedUserId.current = null;
      mergeBlocked.current = false;
      mergeGeneration.current += 1;
      setIsMerging(false);
      setIssue(null);
      return;
    }
    if (mergedUserId.current === userId && guestItems.length === 0) return;

    mergedUserId.current = userId;
    mergeBlocked.current = false;
    const generation = ++mergeGeneration.current;
    const customerSession = captureCustomerSession();
    setIsMerging(true);
    setIssue(null);

    void (async () => {
      let merged = false;
      try {
        await mergeGuestCartExclusive(async () => {
          if (generation !== mergeGeneration.current) return;
          requireMergeCustomerSession(customerSession, userId);

          while (true) {
            requireMergeCustomerSession(customerSession, userId);
            const mergeRequest = getOrCreateGuestCartMergeRequestWhileLocked(userId);
            if (mergeRequest === undefined) break;
            if (mergeRequest.userId !== userId) {
              if (generation === mergeGeneration.current) {
                const mismatch: GuestCartMergeIssue = {
                  kind: "ACCOUNT_MISMATCH",
                  message: "다른 계정으로 로그인하기 전에 담은 상품이 남아 있습니다. 해당 계정으로 다시 로그인해 불러오거나 이 기기에서 해당 상품을 제거해 주세요.",
                  canRetry: false,
                };
                mergeBlocked.current = true;
                setIssue(mismatch);
                toast.show(mismatch.message, "warning");
              }
              return;
            }

            requireMergeCustomerSession(customerSession, userId);
            await mergeGuestCart(
              userId,
              mergeRequest.idempotencyKey,
              mergeRequest.items.map(({ productId, qty }) => ({ productId, qty })),
            );
            requireMergeCustomerSession(customerSession, userId);

            completeGuestCartMergeRequestWhileLocked(mergeRequest.idempotencyKey);
            consumeMergedItemsWhileLocked(mergeRequest.items);
            merged = true;

            if (generation !== mergeGeneration.current) {
              if (mergeBlocked.current) {
                // 이전 계정의 늦은 완료가 현재 계정의 보류 요청을 해소했으므로 다시 조정한다.
                mergedUserId.current = null;
                mergeBlocked.current = false;
                setIssue(null);
                setMergeRevision((revision) => revision + 1);
              }
              return;
            }
            setIssue(null);
          }
        });
      } catch (error) {
        if (error instanceof CustomerSessionChangedError) {
          if (generation === mergeGeneration.current) {
            mergedUserId.current = null;
            mergeBlocked.current = false;
            setIssue(null);
          }
          return;
        }
        if (generation === mergeGeneration.current) {
          const failure: GuestCartMergeIssue = {
            kind: "MERGE_FAILED",
            message: `장바구니를 합치지 못했습니다. ${mergeFailureMessage(error)}`,
            canRetry: true,
          };
          setIssue(failure);
          toast.show(failure.message, "warning");
        }
      } finally {
        if (merged) {
          try {
            requireMergeCustomerSession(customerSession, userId);
            await queryClient.invalidateQueries({
              queryKey: [...queryKeys.member.cart, userId],
            });
            requireMergeCustomerSession(customerSession, userId);
          } catch {
            // 현재 회원 장바구니 query가 자체 오류 상태와 재시도를 제공한다.
          }
        }
        if (generation === mergeGeneration.current) {
          setIsMerging(false);
        }
      }
    })();
  }, [
    consumeMergedItemsWhileLocked,
    customerSessionVersion,
    guestItems,
    mergeRevision,
    queryClient,
    toast,
    userId,
  ]);

  const retry = useCallback(() => {
    if (userId === null) return;
    mergedUserId.current = null;
    mergeBlocked.current = false;
    mergeGeneration.current += 1;
    setIssue(null);
    setIsMerging(true);
    setMergeRevision((revision) => revision + 1);
  }, [userId]);

  const discard = useCallback(() => {
    void editGuestCartExclusive(() => {
      const discarded = discardGuestCartMergeRequestWhileLocked();
      if (discarded) {
        consumeMergedItemsWhileLocked(discarded.items);
      }
    }).then(() => {
      mergedUserId.current = null;
      mergeBlocked.current = false;
      mergeGeneration.current += 1;
      setIsMerging(userId !== null);
      setIssue(null);
      setMergeRevision((revision) => revision + 1);
    });
  }, [consumeMergedItemsWhileLocked, userId]);

  return {
    isMerging,
    issue,
    retry,
    discard,
  };
}
