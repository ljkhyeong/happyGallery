import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Modal } from "react-bootstrap";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingCancelPolicy, CancelResponse } from "@/shared/types";

const PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE =
  "PASS_CREDIT_NOT_RESTORABLE_AFTER_DEADLINE";

function resolvePolicyNotice(cancelPolicy: BookingCancelPolicy) {
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
  onSuccess: () => void;
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
  const [showConfirm, setShowConfirm] = useState(false);
  const policyNotice = resolvePolicyNotice(cancelPolicy);

  const mutation = useMutation({
    mutationFn: onCancel,
    onSuccess: (res) => {
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
      onSuccess();
    },
  });

  return (
    <>
      <Button variant="outline-danger" onClick={() => setShowConfirm(true)}>
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
