import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, Button } from "react-bootstrap";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
import { ConfirmModal, ErrorAlert, useToast } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { CancelResponse } from "@/generated/api/booking";
import type { BookingCancelPolicy } from "@/shared/types";

const PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE =
  "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE";

function isPassBookingPolicy(cancelPolicy: BookingCancelPolicy) {
  return cancelPolicy.passCreditRestorable
    || cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE;
}

function resolvePolicyNotice(
  cancelPolicy: BookingCancelPolicy,
  depositAmount: number,
) {
  if (cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE) {
    return {
      variant: "warning",
      message:
        "취소 마감이 지나 사용한 이용권 1회는 돌려드리지 않습니다.",
    } as const;
  }
  if (cancelPolicy.passCreditRestorable) {
    return {
      variant: "info",
      message: "사용한 이용권 1회를 돌려드립니다.",
    } as const;
  }
  if (cancelPolicy.manualCompensationRequired) {
    return {
      variant: "info",
      message: `예약금 ${formatKRW(depositAmount)}은 공방에서 반환 여부를 확인한 뒤 안내해 드립니다.`,
    } as const;
  }
  if (cancelPolicy.refundable) {
    return {
      variant: "info",
      message: depositAmount > 0
        ? `예약금 ${formatKRW(depositAmount)} 환불이 요청됩니다. 결제사 처리 완료 시점은 별도입니다.`
        : "예약금으로 돌려드릴 금액 없이 예약만 취소됩니다.",
    } as const;
  }
  return {
    variant: "warning",
    message: depositAmount > 0
      ? `취소 마감이 지나 예약금 ${formatKRW(depositAmount)}은 환불되지 않습니다. 예약만 취소됩니다.`
      : "취소 마감이 지나 예약은 취소되지만 돌려드릴 예약금은 없습니다.",
  } as const;
}

function resolveConfirmLabel(
  cancelPolicy: BookingCancelPolicy,
  depositAmount: number,
) {
  if (cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE) {
    return "1회 차감 유지하고 취소";
  }
  if (cancelPolicy.passCreditRestorable) {
    return "예약 취소 및 1회 복원";
  }
  if (cancelPolicy.manualCompensationRequired
    || (cancelPolicy.refundable && depositAmount > 0)) {
    return "취소 및 환불 요청";
  }
  if (cancelPolicy.refundable) {
    return "예약 취소";
  }
  return "환불 없이 예약 취소";
}

interface Props {
  onCancel: () => Promise<CancelResponse>;
  onSuccess: () => void | Promise<void>;
  cancelPolicy: BookingCancelPolicy;
  depositAmount: number;
}

export function CancelButton({
  onCancel,
  onSuccess,
  cancelPolicy,
  depositAmount,
}: Props) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [showConfirm, setShowConfirm] = useState(false);
  const passBooking = isPassBookingPolicy(cancelPolicy);
  const policyNotice = resolvePolicyNotice(cancelPolicy, depositAmount);
  const confirmLabel = resolveConfirmLabel(cancelPolicy, depositAmount);

  const applySuccess = async (
    res: CancelResponse,
    requireCurrent: () => void,
  ) => {
    requireCurrent();
    await Promise.all([
      invalidateSlotAvailability(queryClient),
      onSuccess(),
      passBooking
        ? queryClient.invalidateQueries({ queryKey: queryKeys.member.passes })
        : Promise.resolve(),
    ]);
    requireCurrent();
    setShowConfirm(false);
    let message: string;
    let variant: "success" | "warning" | "info";

    if (passBooking && !res.refundable) {
      message = "예약이 취소되었습니다. 사용한 이용권 1회는 돌려드리지 않습니다.";
      variant = "warning";
    } else if (passBooking) {
      message = "예약이 취소되었습니다. 사용한 이용권 1회를 돌려드렸습니다.";
      variant = "success";
    } else if (res.manualCompensationRequired) {
      message = "예약이 취소되었습니다. 공방에서 예약금 환불을 확인한 뒤 안내해 드립니다.";
      variant = "warning";
    } else if (res.refund?.status === "SUCCEEDED") {
      message = `예약이 취소되었고 ${formatKRW(res.refund.amount)} 환불이 완료되었습니다.`;
      variant = "success";
    } else if (res.refund) {
      message = `예약이 취소되었고 ${formatKRW(res.refund.amount)} 환불 요청이 접수되었습니다.`;
      variant = "info";
    } else if (res.refundable) {
      message = "예약이 취소되었습니다.";
      variant = "success";
    } else {
      message = "예약이 취소되었습니다. 예약금은 환불되지 않았습니다.";
      variant = "warning";
    }

    toast.show(message, variant);
  };

  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      onCancel,
      async (res, requireCurrent) => {
        await applySuccess(res, requireCurrent);
        return res;
      },
    ),
  });

  if (!cancelPolicy.cancellable) {
    // 잔금까지 낸 예약은 고객이 취소할 수 없다. 비활성 버튼으로는 사유가 보이지 않아 안내를 바로 보인다.
    // 공방 연락처는 같은 화면의 예약 정보 칸에 있다.
    return (
      <Alert variant="warning" className="mb-0 py-2 small">
        잔금 결제가 완료된 예약은 취소·인원 변경과 환불을 공방에 문의해 주세요.
      </Alert>
    );
  }

  return (
    <>
      <Button variant="outline-dark" onClick={() => setShowConfirm(true)}>
        예약 취소
      </Button>

      <ConfirmModal
        show={showConfirm}
        title="예약 취소 및 환불 안내"
        pending={false}
        onClose={() => setShowConfirm(false)}
        confirmLabel={mutation.isPending ? "취소 중..." : confirmLabel}
        confirmVariant="dark"
        confirmDisabled={mutation.isPending}
        onConfirm={() => mutation.mutate()}
        cancelVariant="outline-dark"
      >
        <ErrorAlert error={mutation.error} />
        <p className="mb-3">예약을 취소하면 되돌릴 수 없습니다.</p>
        <dl className="row small mb-3">
          <dt className="col-6">환불·이용 횟수 복원 마감</dt>
          <dd className="col-6 text-end mb-0">{formatDateTime(cancelPolicy.deadlineAt)}</dd>
          {!passBooking && (
            <>
              <dt className="col-6 mt-2">예약금</dt>
              <dd className="col-6 text-end mt-2 mb-0">{formatKRW(depositAmount)}</dd>
            </>
          )}
        </dl>
        <Alert variant={policyNotice.variant} className="mb-0 py-2 small">
          {policyNotice.message}
        </Alert>
      </ConfirmModal>
    </>
  );
}
