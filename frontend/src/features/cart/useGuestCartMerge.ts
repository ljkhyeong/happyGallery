import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { ApiError, queryKeys } from "@/shared/api";
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
  guestItems: GuestCartItem[];
  consumeMergedItemsWhileLocked: (items: GuestCartItem[]) => GuestCartItem[];
}

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
  if (error instanceof GuestCartLockUnavailableError) {
    return error.message;
  }
  return "알 수 없는 오류가 발생했습니다.";
}

export function useGuestCartMerge({
  userId,
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
    setIsMerging(true);
    setIssue(null);

    void (async () => {
      let merged = false;
      try {
        await mergeGuestCartExclusive(async () => {
          if (generation !== mergeGeneration.current) return;

          while (true) {
            const mergeRequest = getOrCreateGuestCartMergeRequestWhileLocked(userId);
            if (mergeRequest === undefined) break;
            if (mergeRequest.userId !== userId) {
              if (generation === mergeGeneration.current) {
                const mismatch: GuestCartMergeIssue = {
                  kind: "ACCOUNT_MISMATCH",
                  message: "다른 계정에서 시작한 장바구니 병합이 보류 중입니다. 해당 계정으로 다시 로그인해 재시도하거나 보류 항목을 폐기해 주세요.",
                  canRetry: false,
                };
                mergeBlocked.current = true;
                setIssue(mismatch);
                toast.show(mismatch.message, "warning");
              }
              return;
            }

            await mergeGuestCart(
              mergeRequest.idempotencyKey,
              mergeRequest.items.map(({ productId, qty }) => ({ productId, qty })),
            );
            merged = true;

            completeGuestCartMergeRequestWhileLocked(mergeRequest.idempotencyKey);
            consumeMergedItemsWhileLocked(mergeRequest.items);

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
          await queryClient.invalidateQueries({
            queryKey: [...queryKeys.member.cart, userId],
          });
        }
        if (generation === mergeGeneration.current) {
          setIsMerging(false);
        }
      }
    })();
  }, [
    consumeMergedItemsWhileLocked,
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
      setIsMerging(false);
      setIssue(null);
      setMergeRevision((revision) => revision + 1);
    });
  }, [consumeMergedItemsWhileLocked]);

  return {
    isMerging,
    issue,
    retry,
    discard,
  };
}
