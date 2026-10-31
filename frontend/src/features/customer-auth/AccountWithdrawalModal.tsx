import { useEffect, useState, type FormEvent } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Form, Modal } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { CustomerStepUpPrompt } from "@/features/customer-auth/CustomerStepUpPrompt";
import { useReauthenticationGate } from "@/features/customer-auth/useReauthenticationGate";
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
  // 탈퇴는 먼저 요청하고, 서버가 본인 확인을 요구할 때만 프롬프트로 전환한다.
  const {
    reauthenticated, stepUpBusy, setStepUpBusy, requireStepUpOnError, markVerified, reset: resetStepUp,
  } = useReauthenticationGate({ show, initiallyReauthenticated: true });
  const withdrawal = useMutation({
    mutationFn: onWithdraw,
    onError: requireStepUpOnError,
  });

  useEffect(() => {
    if (show) setAgreed(false);
  }, [show]);

  const busy = withdrawal.isPending || stepUpBusy;

  function close() {
    if (busy) return;
    setAgreed(false);
    resetStepUp();
    withdrawal.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (agreed && !busy) withdrawal.mutate();
  }

  return (
    <Modal
      show={show}
      aria-labelledby="account-withdrawal-title"
      onHide={close}
      backdrop={busy ? "static" : true}
      keyboard={!busy}
      centered
      scrollable
    >
      <Modal.Header closeLabel="창 닫기" closeButton={!busy}>
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
                markVerified();
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
              닫기
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
            <h2 className="h6">회원 탈퇴 유의사항</h2>
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
              label="[필수] 회원 탈퇴 유의사항을 확인했으며 탈퇴에 동의합니다."
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
              닫기
            </Button>
            <Button
              type="submit"
              variant="dark"
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
