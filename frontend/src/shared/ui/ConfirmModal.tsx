import { useId, type ReactNode } from "react";
import { Button, Modal, type ButtonProps, type ModalProps } from "react-bootstrap";

interface Props {
  show: boolean;
  title: ReactNode;
  /** 요청 중에는 닫기와 확인을 막는다. */
  pending: boolean;
  onClose: () => void;
  confirmLabel: ReactNode;
  confirmVariant: ButtonProps["variant"];
  /** 요청 중 여부와 별개로 확인을 막을 조건 */
  confirmDisabled?: boolean;
  onConfirm: () => void;
  /** 머리글 닫기 버튼의 접근성 이름. 비우면 react-bootstrap 기본값을 쓴다. */
  closeLabel?: string;
  cancelLabel?: string;
  cancelVariant?: ButtonProps["variant"];
  /** 닫기와 확인 사이에 둘 다른 처리 버튼 */
  extraActions?: ReactNode;
  /** 배경 클릭·Esc 닫기 동작. 요청 중 static 배경으로 닫히지 않음을 알릴 때만 넘기고, 비우면 react-bootstrap 기본값을 쓴다. */
  backdrop?: ModalProps["backdrop"];
  keyboard?: ModalProps["keyboard"];
  children: ReactNode;
}

/** 되돌리기 어려운 처리를 실행하기 전에 내용을 확인하는 창. 관리자·고객 화면이 함께 쓴다. */
export function ConfirmModal({
  show,
  title,
  pending,
  onClose,
  confirmLabel,
  confirmVariant,
  confirmDisabled = false,
  onConfirm,
  closeLabel,
  cancelLabel = "닫기",
  cancelVariant = "outline-secondary",
  extraActions,
  backdrop,
  keyboard,
  children,
}: Props) {
  const titleId = useId();

  return (
    <Modal
      show={show}
      aria-labelledby={titleId}
      onHide={() => {
        if (!pending) onClose();
      }}
      backdrop={backdrop}
      keyboard={keyboard}
      centered
    >
      <Modal.Header closeButton={!pending} closeLabel={closeLabel}>
        <Modal.Title id={titleId} className="fs-6">{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>{children}</Modal.Body>
      <Modal.Footer>
        <Button variant={cancelVariant} disabled={pending} onClick={onClose}>
          {cancelLabel}
        </Button>
        {extraActions}
        <Button
          variant={confirmVariant}
          disabled={pending || confirmDisabled}
          onClick={onConfirm}
        >
          {confirmLabel}
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
