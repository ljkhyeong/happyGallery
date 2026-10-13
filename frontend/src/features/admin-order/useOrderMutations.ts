import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useToast } from "@/shared/ui";
import {
  approveOrder, rejectOrder, completeProduction,
  proposeDelay, cancelForDelayRejection, resumeAfterDelay, preparePickup, completePickup,
  refundMissedPickup,
  setExpectedShipDate, expirePickups,
  prepareShipping, markShipped, markDelivered,
} from "./api";
import type { MarkPickupReadyRequest, MarkShippedRequest, SetExpectedShipDateRequest } from "@/shared/types";
import { useAdminRefundPolling } from "@/features/admin-refund/useAdminRefundPolling";

interface UseOrderMutationsOptions {
  adminKey: string;
  onAuthError: () => void;
  onInvalidate: () => void;
}

export function useOrderMutations({ adminKey, onAuthError, onInvalidate }: UseOrderMutationsOptions) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { trackRefund } = useAdminRefundPolling(adminKey, onAuthError);
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [lastError, setLastError] = useState<Error | null>(null);

  function invalidate() {
    onInvalidate();
    queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
  }

  function startOrderAction(id: number) {
    setPendingId(id);
    setLastError(null);
  }

  function useOrderActionMutation<T>(
    fn: (id: number) => Promise<T>,
    label: string,
  ) {
    return useAdminMutation(onAuthError, {
      mutationFn: fn,
      onMutate: startOrderAction,
      onSuccess: (_: T, id: number) => { toast.show(`주문 #${id} ${label}`); invalidate(); },
      onError: setLastError,
      onSettled: () => setPendingId(null),
    });
  }

  /** 환불을 요청하는 처리. 결과 환불 건을 추적해 완료·실패를 알린다. */
  function useOrderRefundMutation<T extends { refund: { refundId: number } }>(
    fn: (id: number) => Promise<T>,
    message: string,
  ) {
    return useAdminMutation(onAuthError, {
      mutationFn: fn,
      onMutate: startOrderAction,
      onSuccess: (result: T, id: number) => {
        toast.show(`주문 #${id} ${message}`, "info");
        trackRefund(result.refund.refundId, `주문 #${id}`);
        invalidate();
      },
      onError: setLastError,
      onSettled: () => setPendingId(null),
    });
  }

  /** 입력값(수령 마감·운송장·출고일)을 함께 보내는 처리 */
  function useOrderInputMutation<TBody, T>(
    fn: (id: number, body: TBody) => Promise<T>,
    label: string,
  ) {
    return useAdminMutation(onAuthError, {
      mutationFn: ({ id, body }: { id: number; body: TBody }) => fn(id, body),
      onMutate: ({ id }) => startOrderAction(id),
      onSuccess: (_: T, { id }) => { toast.show(`주문 #${id} ${label}`); invalidate(); },
      onError: setLastError,
      onSettled: () => setPendingId(null),
    });
  }

  const approve = useOrderActionMutation((id) => approveOrder(adminKey, id), "승인 완료");
  const reject = useOrderRefundMutation(
    (id) => rejectOrder(adminKey, id),
    "거절 및 환불 요청이 접수되었습니다.",
  );
  const completeProduction_ = useOrderActionMutation((id) => completeProduction(adminKey, id), "제작 완료");
  const delay = useOrderActionMutation((id) => proposeDelay(adminKey, id), "지연 동의 요청");
  const delayCancel = useOrderRefundMutation(
    (id) => cancelForDelayRejection(adminKey, id),
    "지연 거절 취소 및 환불 요청이 접수되었습니다.",
  );
  const resumeAfterDelay_ = useOrderActionMutation(
    (id) => resumeAfterDelay(adminKey, id),
    "제작 지연 동의 후 주문 처리 재개",
  );
  const prepareShipping_ = useOrderActionMutation((id) => prepareShipping(adminKey, id), "배송 준비");
  const delivered = useOrderActionMutation((id) => markDelivered(adminKey, id), "배송 완료");
  const pickupDone = useOrderActionMutation((id) => completePickup(adminKey, id), "매장 수령 완료");
  const missedPickupRefund = useOrderRefundMutation(
    (id) => refundMissedPickup(adminKey, id),
    "미수령 예외 환불 요청이 접수되었습니다.",
  );

  const pickup = useOrderInputMutation(
    (id, body: MarkPickupReadyRequest) => preparePickup(adminKey, id, body),
    "매장 수령 준비 완료",
  );
  const shipped = useOrderInputMutation(
    (id, body: MarkShippedRequest) => markShipped(adminKey, id, body),
    "배송 출발",
  );
  const shipDate = useOrderInputMutation(
    (id, body: SetExpectedShipDateRequest) => setExpectedShipDate(adminKey, id, body),
    "출고일 설정",
  );

  const expire = useAdminMutation(onAuthError, {
    mutationFn: () => expirePickups(adminKey),
    onMutate: () => setLastError(null),
    onSuccess: (r) => {
      toast.show(`수령 기한 지난 주문 미수령 처리: 완료 ${r.successCount}건 · 확인 필요 ${r.failureCount}건`);
      invalidate();
    },
    onError: setLastError,
  });

  return {
    pendingId,
    approve, reject, completeProduction: completeProduction_, delay, delayCancel,
    resumeAfterDelay: resumeAfterDelay_,
    prepareShipping: prepareShipping_, shipped, delivered, pickup, pickupDone,
    missedPickupRefund, shipDate, expire,
    lastError,
  } as const;
}

export type OrderMutations = ReturnType<typeof useOrderMutations>;
