import { useEffect, useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, Modal } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { CustomerStepUpPrompt } from "@/features/customer-auth/CustomerStepUpPrompt";
import { ErrorAlert } from "@/shared/ui";

const CONFIRMATION = "탈퇴";

interface Props {
  show: boolean;
  localPasswordEnabled: boolean;
  onClose: () => void;
  onWithdraw: () => Promise<void>;
}

export function AccountWithdrawalModal({
  show,
  localPasswordEnabled,
  onClose,
  onWithdraw,
}: Props) {
  const [confirmation, setConfirmation] = useState("");
  const [reauthenticated, setReauthenticated] = useState(true);
  const [stepUpBusy, setStepUpBusy] = useState(false);
  const withdrawal = useMutation({
    mutationFn: onWithdraw,
    onError: (error) => {
      if (error instanceof ApiError && error.code === "REAUTHENTICATION_REQUIRED") {
        setReauthenticated(false);
      }
    },
  });

  useEffect(() => {
    if (show) {
      setReauthenticated(true);
    }
  }, [show]);

  function close() {
    if (withdrawal.isPending || stepUpBusy) return;
    setConfirmation("");
    setReauthenticated(true);
    withdrawal.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (confirmation === CONFIRMATION) withdrawal.mutate();
  }

  return (
    <Modal
      show={show}
      onHide={close}
      backdrop={withdrawal.isPending || stepUpBusy ? "static" : true}
      keyboard={!withdrawal.isPending && !stepUpBusy}
      centered
    >
      <Modal.Header closeButton={!withdrawal.isPending && !stepUpBusy}>
        <Modal.Title className="fs-6">회원 탈퇴</Modal.Title>
      </Modal.Header>
      {!reauthenticated ? (
        <>
          <Modal.Body>
            <p className="small">
              회원 탈퇴를 계속하려면 본인 확인이 필요합니다.
            </p>
            <CustomerStepUpPrompt
              localPasswordEnabled={localPasswordEnabled}
              returnAction="account-withdrawal"
              onVerified={() => {
                withdrawal.reset();
                setReauthenticated(true);
              }}
              onBusyChange={setStepUpBusy}
            />
          </Modal.Body>
          <Modal.Footer>
            <Button
              variant="outline-secondary"
              onClick={close}
              disabled={stepUpBusy}
            >
              취소
            </Button>
          </Modal.Footer>
        </>
      ) : (
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
            <Button
              variant="outline-secondary"
              onClick={close}
              disabled={withdrawal.isPending}
            >
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
      )}
    </Modal>
  );
}
