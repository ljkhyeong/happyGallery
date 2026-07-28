import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Modal } from "react-bootstrap";
import { invalidateSlotAvailability, runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingCancelPolicy, CancelResponse } from "@/shared/types";

const PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE =
  "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE";

function resolvePolicyNotice(cancelPolicy: BookingCancelPolicy) {
  if (!cancelPolicy.cancellable) {
    return {
      variant: "warning",
      message: "잔금 결제가 완료된 예약은 관리자에게 취소와 정산을 요청해 주세요.",
    } as const;
  }
  if (cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE) {
    return {
      variant: "warning",
      message:
        "취소 마감이 지나 8회권 크레딧은 복구되지 않습니다. 취소 후에도 사용 횟수는 차감된 상태로 유지됩니다.",
    } as const;
  }
  if (cancelPolicy.passCreditRestorable) {
    return {
      variant: "info",
      message: "취소하면 사용한 8회권 크레딧 1회가 복구됩니다.",
    } as const;
  }
  if (cancelPolicy.manualCompensationRequired) {
    return {
      variant: "info",
      message: "취소 후 운영자가 오프라인 예약금 반환을 확인해 안내합니다.",
    } as const;
  }
  if (cancelPolicy.refundable) {
    return { variant: "info", message: "취소 마감 전이므로 예약금 환불이 요청됩니다." } as const;
  }
  return {
    variant: "warning",
    message: "취소 마감이 지나 예약금은 환불되지 않습니다.",
  } as const;
}

interface Props {
  onCancel: () => Promise<CancelResponse>;
  onSuccess: () => void | Promise<void>;
  cancelPolicy: BookingCancelPolicy;
  buttonLabel?: string;
}

export function CancelButton({
  onCancel,
  onSuccess,
  cancelPolicy,
  buttonLabel = "예약 취소",
}: Props) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [showConfirm, setShowConfirm] = useState(false);
  const policyNotice = resolvePolicyNotice(cancelPolicy);

  const applySuccess = async (
    res: CancelResponse,
    requireCurrent: () => void,
  ) => {
    requireCurrent();
    await invalidateSlotAvailability(queryClient);
    requireCurrent();
    await onSuccess();
    requireCurrent();
    setShowConfirm(false);
    const passCreditNotRestored =
      cancelPolicy.warningCode === PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE;
    const passBooking = passCreditNotRestored || cancelPolicy.passCreditRestorable;
    let message: string;
    let variant: "success" | "warning" | "info";

    if (passBooking && !res.refundable) {
      message = "예약이 취소되었습니다. 취소 마감이 지나 8회권 크레딧은 복구되지 않았습니다.";
      variant = "warning";
    } else if (passBooking) {
      message = "예약이 취소되었고 8회권 크레딧 1회가 복구되었습니다.";
      variant = "success";
    } else if (res.manualCompensationRequired) {
      message = "예약이 취소되었습니다. 운영자가 오프라인 예약금 반환을 확인한 뒤 안내합니다.";
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
        title={!cancelPolicy.cancellable ? "관리자 정산이 필요한 예약입니다." : undefined}
        onClick={() => setShowConfirm(true)}
      >
        {buttonLabel}
      </Button>

      <Modal show={showConfirm} onHide={() => setShowConfirm(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>예약 취소</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={mutation.error} />
          <p>정말 예약을 취소하시겠습니까?</p>
          <Alert variant={policyNotice.variant} className="mb-0 py-2 small">
            {policyNotice.message}
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowConfirm(false)}>
            닫기
          </Button>
          <Button variant="danger" disabled={mutation.isPending} onClick={() => mutation.mutate()}>
            {mutation.isPending ? "취소 중..." : "취소 확인"}
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
