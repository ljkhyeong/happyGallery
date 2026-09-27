import { useEffect, useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { CustomerStepUpPrompt } from "@/features/customer-auth/CustomerStepUpPrompt";
import { ErrorAlert } from "@/shared/ui";

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
  const [agreed, setAgreed] = useState(false);
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
      setAgreed(false);
      setReauthenticated(true);
    }
  }, [show]);

  function close() {
    if (withdrawal.isPending || stepUpBusy) return;
    setAgreed(false);
    setReauthenticated(true);
    withdrawal.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (agreed && !withdrawal.isPending && !stepUpBusy) withdrawal.mutate();
  }

  return (
    <Modal
      show={show}
      aria-labelledby="account-withdrawal-title"
      onHide={close}
      backdrop={withdrawal.isPending || stepUpBusy ? "static" : true}
      keyboard={!withdrawal.isPending && !stepUpBusy}
      centered
      scrollable
    >
      <Modal.Header closeButton={!withdrawal.isPending && !stepUpBusy}>
        <Modal.Title id="account-withdrawal-title" className="fs-6">회원 탈퇴</Modal.Title>
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
        <Form onSubmit={submit} className="d-flex flex-column overflow-hidden">
          <Modal.Body>
            {withdrawal.error instanceof ApiError && withdrawal.error.is("ACCOUNT_WITHDRAWAL_BLOCKED") ? (
              <Alert variant="danger" role="alert">
                <p className="mb-2">다음 내역이 있어 탈퇴할 수 없습니다.</p>
                <ul className="mb-0">
                  {withdrawal.error.message.split("\n").map((reason) => <li key={reason}>{reason}</li>)}
                </ul>
              </Alert>
            ) : <ErrorAlert error={withdrawal.error} />}
            <h2 className="h6">회원탈퇴 유의사항</h2>
            <p className="small text-muted">탈퇴하기 전에 아래 내용을 확인해 주세요.</p>
            <ul className="small ps-3">
              <li className="mb-2">
                탈퇴하면 이 계정으로 로그인할 수 없으며, 계정 개인정보와 비밀번호가
                삭제·익명화되고 소셜 계정 연결이 해제됩니다.
              </li>
              <li className="mb-2">
                사용하지 않은 쿠폰과 적립금은 더 이상 사용할 수 없습니다.
                재가입해도 기존 계정과 혜택은 복구되지 않습니다.
              </li>
              <li className="mb-2">
                주문·예약 등 거래 기록과 혜택 사용·정산 이력은 개인정보 처리방침에 따라 보존됩니다.
              </li>
              <li>
                진행 중인 결제·주문·반품·교환·예약·환불, 사용 가능한 이용권이나
                정산할 적립금이 있으면 해당 처리가 끝난 뒤 탈퇴할 수 있습니다.
                탈퇴 신청으로 자동 취소되거나 환불되지 않습니다.
              </li>
            </ul>
            <p className="small text-muted">
              탈퇴가 제한되면 먼저 처리해야 할 내역을 안내합니다.
              필요한 경우 본인 확인을 다시 진행합니다.
            </p>
            <Form.Check
              id="withdrawal-agreement"
              type="checkbox"
              label="회원탈퇴 유의사항을 확인했으며, 탈퇴에 동의합니다. (필수)"
              checked={agreed}
              onChange={(event) => setAgreed(event.target.checked)}
              disabled={withdrawal.isPending}
              required
            />
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
              disabled={!agreed || withdrawal.isPending}
            >
              {withdrawal.isPending ? "처리 중..." : "동의하고 탈퇴하기"}
            </Button>
          </Modal.Footer>
        </Form>
      )}
    </Modal>
  );
}
