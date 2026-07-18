import { useState, useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useToast } from "@/shared/ui";
import {
  approveOrder, rejectOrder, completeProduction,
  requestDelay, cancelForDelayRejection, resumeProduction, preparePickup, completePickup,
  setExpectedShipDate, expirePickups,
  prepareShipping, markShipped, markDelivered,
} from "./api";
import type { MarkPickupReadyRequest, SetExpectedShipDateRequest } from "@/shared/types";
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

  const invalidate = useCallback(() => {
    onInvalidate();
    queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
  }, [onInvalidate, queryClient]);

  function mut<T>(
    fn: (id: number) => Promise<T>,
    label: string,
  ) {
    return useAdminMutation(onAuthError, {
      mutationFn: fn,
      onMutate: (id: number) => setPendingId(id),
      onSuccess: (_: T, id: number) => { toast.show(`주문 #${id} ${label}`); invalidate(); },
      onSettled: () => setPendingId(null),
    });
  }

  const approve = mut((id) => approveOrder(adminKey, id), "승인 완료");
  const reject = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => rejectOrder(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (result, id) => {
      toast.show(`주문 #${id} 거절 및 환불 요청이 접수되었습니다.`, "info");
      trackRefund(result.refund.refundId, `주문 #${id}`);
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });
  const completeProduction_ = mut((id) => completeProduction(adminKey, id), "제작 완료");
  const delay = mut((id) => requestDelay(adminKey, id), "지연 요청");
  const delayCancel = useAdminMutation(onAuthError, {
    mutationFn: (id: number) => cancelForDelayRejection(adminKey, id),
    onMutate: (id) => setPendingId(id),
    onSuccess: (result, id) => {
      toast.show(`주문 #${id} 지연 거절 취소 및 환불 요청이 접수되었습니다.`, "info");
      trackRefund(result.refund.refundId, `주문 #${id}`);
      invalidate();
    },
    onSettled: () => setPendingId(null),
  });
  const resumeProduction_ = mut((id) => resumeProduction(adminKey, id), "제작 재개");
  const prepareShipping_ = mut((id) => prepareShipping(adminKey, id), "배송 준비");
  const shipped = mut((id) => markShipped(adminKey, id), "배송 출발");
  const delivered = mut((id) => markDelivered(adminKey, id), "배송 완료");
  const pickupDone = mut((id) => completePickup(adminKey, id), "픽업 완료");

  const pickup = useAdminMutation(onAuthError, {
    mutationFn: ({ id, body }: { id: number; body: MarkPickupReadyRequest }) => preparePickup(adminKey, id, body),
    onMutate: ({ id }) => setPendingId(id),
    onSuccess: (_, { id }) => { toast.show(`주문 #${id} 픽업 준비 완료`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const shipDate = useAdminMutation(onAuthError, {
    mutationFn: ({ id, body }: { id: number; body: SetExpectedShipDateRequest }) => setExpectedShipDate(adminKey, id, body),
    onMutate: ({ id }) => setPendingId(id),
    onSuccess: (_, { id }) => { toast.show(`주문 #${id} 출고일 설정`); invalidate(); },
    onSettled: () => setPendingId(null),
  });

  const expire = useAdminMutation(onAuthError, {
    mutationFn: () => expirePickups(adminKey),
    onSuccess: (r) => { toast.show(`픽업 만료 배치: 성공 ${r.successCount}, 실패 ${r.failureCount}`); invalidate(); },
  });

  const lastError = approve.error ?? reject.error ?? completeProduction_.error
    ?? delay.error ?? delayCancel.error ?? resumeProduction_.error ?? pickup.error ?? pickupDone.error ?? shipDate.error
    ?? prepareShipping_.error ?? shipped.error ?? delivered.error ?? expire.error;

  return {
    pendingId,
    approve, reject, completeProduction: completeProduction_, delay, delayCancel, resumeProduction: resumeProduction_,
    prepareShipping: prepareShipping_, shipped, delivered, pickup, pickupDone, shipDate, expire,
    lastError,
  } as const;
}

export type OrderMutations = ReturnType<typeof useOrderMutations>;
