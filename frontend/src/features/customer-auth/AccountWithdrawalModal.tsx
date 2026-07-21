import { useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, Modal } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";

const CONFIRMATION = "탈퇴";

interface Props {
  show: boolean;
  onClose: () => void;
  onWithdraw: () => Promise<void>;
}

export function AccountWithdrawalModal({ show, onClose, onWithdraw }: Props) {
  const [confirmation, setConfirmation] = useState("");
  const withdrawal = useMutation({ mutationFn: onWithdraw });

  function close() {
    if (withdrawal.isPending) return;
    setConfirmation("");
    withdrawal.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (confirmation === CONFIRMATION) withdrawal.mutate();
  }

  return (
    <Modal show={show} onHide={close} centered>
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">회원 탈퇴</Modal.Title>
      </Modal.Header>
      <Form onSubmit={submit}>
        <Modal.Body>
          <ErrorAlert error={withdrawal.error} />
          <p className="small">
            탈퇴하면 계정과 소셜 로그인이 해제되고 개인정보가 익명화됩니다. 주문과 예약의 거래 기록은 보존됩니다.
          </p>
          <Form.Group controlId="withdrawal-confirmation">
            <Form.Label>
              계속하려면 <strong>{CONFIRMATION}</strong>를 입력하세요.
            </Form.Label>
            <Form.Control
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              autoComplete="off"
              autoFocus
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-secondary" onClick={close} disabled={withdrawal.isPending}>
            취소
          </Button>
          <Button
            type="submit"
            variant="danger"
            disabled={confirmation !== CONFIRMATION || withdrawal.isPending}
          >
            {withdrawal.isPending ? "처리 중..." : "회원 탈퇴"}
          </Button>
        </Modal.Footer>
      </Form>
    </Modal>
  );
}
