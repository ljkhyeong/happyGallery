import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Modal } from "react-bootstrap";
import { ErrorAlert, useToast } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { CancelResponse } from "@/shared/types";

interface Props {
  onCancel: () => Promise<CancelResponse>;
  onSuccess: () => void;
  buttonLabel?: string;
}

export function CancelButton({
  onCancel,
  onSuccess,
  buttonLabel = "예약 취소",
}: Props) {
  const toast = useToast();
  const [showConfirm, setShowConfirm] = useState(false);

  const mutation = useMutation({
    mutationFn: onCancel,
    onSuccess: (res) => {
      setShowConfirm(false);
      const msg = res.refundable
        ? `예약이 취소되었습니다. 환불 금액: ${formatKRW(res.refundAmount)}`
        : "예약이 취소되었습니다. (환불 불가 기간)";
      toast.show(msg, res.refundable ? "success" : "warning");
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
          <p className="text-muted-soft small">
            D-1(전날 00:00) 이후에는 예약금 환불이 불가합니다.
          </p>
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
