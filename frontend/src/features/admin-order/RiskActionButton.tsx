import { useState } from "react";
import { Alert, Button, type ButtonProps } from "react-bootstrap";
import { ConfirmModal } from "@/shared/ui/ConfirmModal";

interface Props {
  buttonLabel: string;
  confirmLabel: string;
  title: string;
  impact: string;
  disabled: boolean;
  pending: boolean;
  onConfirm: () => void;
  /** 확인 창을 여는 버튼 색. 실행 버튼은 항상 danger다. */
  variant?: ButtonProps["variant"];
}

/** 환불·재고처럼 되돌리기 어려운 주문 처리를 영향 안내 확인 뒤 실행한다. */
export function RiskActionButton({
  buttonLabel,
  confirmLabel,
  title,
  impact,
  disabled,
  pending,
  onConfirm,
  variant = "outline-danger",
}: Props) {
  const [show, setShow] = useState(false);

  return (
    <>
      <Button
        size="sm"
        variant={variant}
        disabled={disabled}
        onClick={() => setShow(true)}
      >
        {pending ? "처리 중..." : buttonLabel}
      </Button>
      <ConfirmModal
        show={show}
        title={title}
        pending={pending}
        onClose={() => setShow(false)}
        confirmLabel={confirmLabel}
        confirmVariant="danger"
        onConfirm={() => {
          setShow(false);
          onConfirm();
        }}
      >
        <Alert variant="warning" className="mb-0 small">
          {impact}
        </Alert>
      </ConfirmModal>
    </>
  );
}
