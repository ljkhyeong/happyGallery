import { useId, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Modal } from "react-bootstrap";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { BookingCancelPolicy, CancelResponse } from "@/shared/types";

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
  if (!cancelPolicy.cancellable) {
    return {
      variant: "warning",
      message: "잔금 결제가 완료된 예약은 공방에 취소와 환불을 문의해 주세요.",
    } as const;
  }
  if (cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE) {
    return {
      variant: "warning",
      message:
        "취소 마감이 지나 사용한 8회권 1회는 돌려드리지 않습니다. 취소 후에도 이용 횟수는 차감된 상태로 유지됩니다.",
    } as const;
  }
  if (cancelPolicy.passCreditRestorable) {
    return {
      variant: "info",
      message: "사용한 8회권 1회가 이용 가능 횟수로 복구됩니다.",
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
    return "예약 취소 및 1회 복구";
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
  buttonLabel?: string;
}

export function CancelButton({
  onCancel,
  onSuccess,
  cancelPolicy,
  depositAmount,
  buttonLabel = "예약 취소",
}: Props) {
  const titleId = `booking-cancel-title-${useId()}`;
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
    const passCreditNotRestored =
      cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE;
    const passBooking = passCreditNotRestored || cancelPolicy.passCreditRestorable;
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
      message = "예약이 취소되었습니다. 사용한 8회권 1회는 복구되지 않았습니다.";
      variant = "warning";
    } else if (passBooking) {
      message = "예약이 취소되었고 사용한 8회권 1회가 이용 가능 횟수에 다시 반영되었습니다.";
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

  return (
    <>
      <Button
        variant="outline-danger"
        disabled={!cancelPolicy.cancellable}
        title={!cancelPolicy.cancellable ? "공방 확인이 필요한 예약입니다." : undefined}
        onClick={() => setShowConfirm(true)}
      >
        {buttonLabel}
      </Button>

      <Modal
        show={showConfirm}
        aria-labelledby={titleId}
        onHide={() => setShowConfirm(false)}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title id={titleId}>예약 취소 및 환불 안내</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <p className="mb-3">예약을 취소하면 되돌릴 수 없습니다. 아래 내용을 확인해 주세요.</p>
          <dl className="row small mb-3">
            <dt className="col-6">환불·크레딧 복구 마감</dt>
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
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>
            닫기
          </Button>
          <Button variant="danger" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending ? "취소 중..." : confirmLabel}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
