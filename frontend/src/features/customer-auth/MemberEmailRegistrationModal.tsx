import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Form, Modal } from "react-bootstrap";
import { ApiError, runForCurrentCustomer } from "@/shared/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { CustomerStepUpPrompt } from "@/features/customer-auth/CustomerStepUpPrompt";
import { ErrorAlert, useToast } from "@/shared/ui";
import {
  registerVerifiedEmail,
  sendEmailVerification,
} from "./emailRegistrationApi";

interface Props {
  show: boolean;
  localPasswordEnabled: boolean;
  initiallyReauthenticated: boolean;
  onClose: () => void;
}

export function MemberEmailRegistrationModal({
  show,
  localPasswordEnabled,
  initiallyReauthenticated,
  onClose,
}: Props) {
  const toast = useToast();
  const [stepUpBusy, setStepUpBusy] = useState(false);
  const [reauthenticated, setReauthenticated] = useState(initiallyReauthenticated);
  const [email, setEmail] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [sentEmail, setSentEmail] = useState<string | null>(null);

  useEffect(() => {
    if (!show) return;
    setReauthenticated(initiallyReauthenticated);
    setEmail("");
    setVerificationCode("");
    setSentEmail(null);
  }, [show, initiallyReauthenticated]);

  const sendCode = useMutation({
    mutationFn: (targetEmail: string) =>
      runForCurrentCustomer(() => sendEmailVerification(targetEmail)),
    onSuccess: (_, targetEmail) => {
      setSentEmail(targetEmail);
      setVerificationCode("");
      toast.show("인증번호를 이메일로 보냈습니다.");
    },
    onError: handleReauthenticationRequired,
  });

  const register = useMutation({
    mutationFn: ({ targetEmail, code }: { targetEmail: string; code: string }) =>
      runForCurrentCustomer(
        () => registerVerifiedEmail(targetEmail, code),
        () => {
          toast.show("이메일이 등록되었습니다. 다시 로그인해 주세요.");
          window.location.assign(buildAuthPageHref("/login", { redirectTo: "/my" }));
        },
      ),
    onError: handleReauthenticationRequired,
  });

  function handleReauthenticationRequired(error: Error) {
    if (error instanceof ApiError && error.code === "REAUTHENTICATION_REQUIRED") {
      setReauthenticated(false);
    }
  }

  function close() {
    if (busy) return;
    sendCode.reset();
    register.reset();
    onClose();
  }

  const normalizedEmail = email.trim().toLowerCase();
  const emailChangedAfterSend = sentEmail !== null && sentEmail !== normalizedEmail;
  const busy = stepUpBusy || sendCode.isPending || register.isPending;

  return (
    <Modal
      show={show}
      aria-labelledby="member-email-registration-title"
      onHide={close}
      backdrop={busy ? "static" : true}
      keyboard={!busy}
      centered
    >
      <Modal.Header closeButton={!busy}>
        <Modal.Title id="member-email-registration-title" className="fs-6">
          로그인 이메일 등록
        </Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p className="text-muted-soft small mb-3">
          메일함에서 인증번호를 확인하면 이 이메일을 계정에 등록합니다.
        </p>
        <ErrorAlert
          error={reauthenticated ? register.error ?? sendCode.error : null}
        />
        {!reauthenticated ? (
          <CustomerStepUpPrompt
            localPasswordEnabled={localPasswordEnabled}
            returnAction="email-registration"
            onVerified={() => setReauthenticated(true)}
            onBusyChange={setStepUpBusy}
          />
        ) : (
          <>
            <Form
              onSubmit={(event) => {
                event.preventDefault();
                if (!normalizedEmail) return;
                sendCode.mutate(normalizedEmail);
              }}
            >
              <Form.Group controlId="member-email-registration-email">
                <Form.Label>이메일</Form.Label>
                <Form.Control
                  type="email"
                  autoComplete="email"
                  maxLength={254}
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  aria-describedby="member-email-registration-email-help"
                  required
                  autoFocus
                />
                <Form.Text id="member-email-registration-email-help">
                  인증번호를 받을 수 있는 이메일을 입력해 주세요.
                </Form.Text>
              </Form.Group>
              <Button
                type="submit"
                variant={sentEmail ? "outline-primary" : "primary"}
                className="mt-3 w-100"
                disabled={!normalizedEmail || busy}
              >
                {sentEmail ? "인증번호 다시 보내기" : "인증번호 보내기"}
              </Button>
            </Form>

            {sentEmail && !emailChangedAfterSend && (
              <Form
                className="border-top mt-4 pt-4"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!/^[0-9]{6}$/.test(verificationCode)) return;
                  register.mutate({
                    targetEmail: sentEmail,
                    code: verificationCode,
                  });
                }}
              >
                <Form.Group controlId="member-email-registration-code">
                  <Form.Label>인증번호</Form.Label>
                  <Form.Control
                    inputMode="numeric"
                    autoComplete="one-time-code"
                    maxLength={6}
                    pattern="[0-9]{6}"
                    value={verificationCode}
                    onChange={(event) =>
                      setVerificationCode(event.target.value.replace(/\D/g, "").slice(0, 6))
                    }
                    aria-describedby="member-email-registration-code-help"
                    required
                  />
                  <Form.Text id="member-email-registration-code-help">
                    5분 안에 6자리 인증번호를 입력해 주세요.
                  </Form.Text>
                </Form.Group>
                <Button
                  type="submit"
                  className="mt-3 w-100"
                  disabled={!/^[0-9]{6}$/.test(verificationCode) || busy}
                >
                  인증하고 등록
                </Button>
              </Form>
            )}
          </>
        )}
      </Modal.Body>
      <Modal.Footer>
        <Button variant="secondary" onClick={close} disabled={busy}>
          취소
        </Button>
      </Modal.Footer>
    </Modal>
  );
}
