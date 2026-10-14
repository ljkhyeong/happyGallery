import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { useEffect, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Modal, Button, Form, Nav, Alert } from "react-bootstrap";
import { useCustomerAuth, type CustomerUser } from "./useCustomerAuth";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { normalizePhone } from "@/shared/validation/phone";
import { isPasswordWithinByteLimit, PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";
import { ErrorAlert } from "@/shared/ui";
import { PolicyConsentFields } from "@/features/policy-consent/PolicyConsentFields";
import { usePolicyAcceptance } from "@/features/policy-consent/usePolicyAcceptance";
import type { PolicyAcceptance } from "@/features/policy-consent/types";

type AuthPath = "login" | "signup" | "guest";

/** 비회원으로 진행할 때 결제 요청에 넘기는 인증·동의 정보. */
export interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
  policyAcceptance: PolicyAcceptance;
}

interface Props {
  show: boolean;
  onClose: () => void;
  onMemberAuthenticated: (member: CustomerUser) => void;
  onGuestConfirm: (info: GuestInfo) => void;
  guestVerificationReset: { version: number; message: string };
}

export function AuthGateModal({ show, onClose, onMemberAuthenticated, onGuestConfirm, guestVerificationReset }: Props) {
  const { isAuthenticated, login, signup, user } = useCustomerAuth();
  const [tab, setTab] = useState<AuthPath>("login");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [signupName, setSignupName] = useState("");
  const [signupPhone, setSignupPhone] = useState("");
  const [signupVerificationCode, setSignupVerificationCode] = useState("");
  const policyConsent = usePolicyAcceptance();
  // 로그인·가입 탭이 오류와 진행 상태를 함께 쓰도록 요청 하나로 처리한다.
  const authMutation = useMutation({
    mutationFn: (request: { kind: "login" } | { kind: "signup"; acceptance: PolicyAcceptance }) =>
      request.kind === "login"
        ? login(email, password)
        : signup(
          email,
          password,
          signupName,
          signupPhone,
          signupVerificationCode,
          request.acceptance,
        ),
    onSuccess: (member) => onMemberAuthenticated(member),
  });
  const error = authMutation.error;
  const submitting = authMutation.isPending;

  // guest step
  const [guestVerified, setGuestVerified] = useState(false);
  const [guestPhone, setGuestPhone] = useState("");
  const [guestCode, setGuestCode] = useState("");
  const [guestName, setGuestName] = useState("");
  const [guestNameTouched, setGuestNameTouched] = useState(false);
  const normalizedGuestName = guestName.trim();

  useEffect(() => {
    setGuestVerified(false);
    setGuestCode("");
  }, [guestVerificationReset.version]);

  // If already authenticated, confirm directly
  if (isAuthenticated && show) {
    return (
      <Modal
        show={show}
        aria-labelledby="auth-member-confirm-title"
        onHide={onClose}
        centered
      >
        <Modal.Header closeButton>
          <Modal.Title id="auth-member-confirm-title">확인</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0">
            <strong>{user!.name}</strong>님으로 진행합니다.
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="outline-dark" onClick={onClose}>취소</Button>
          <Button variant="primary" onClick={() => onMemberAuthenticated(user!)}>확인</Button>
        </Modal.Footer>
      </Modal>
    );
  }

  function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    if (!isPasswordWithinByteLimit(password)) return;
    authMutation.mutate({ kind: "login" });
  }

  function handleSignup(e: React.FormEvent) {
    e.preventDefault();
    if (!policyConsent.acceptance || !isPasswordWithinByteLimit(password)) {
      return;
    }
    authMutation.mutate({ kind: "signup", acceptance: policyConsent.acceptance });
  }

  function handleGuestSubmit() {
    if (normalizedGuestName && policyConsent.acceptance) {
      onGuestConfirm({
        phone: guestPhone,
        verificationCode: guestCode,
        name: normalizedGuestName,
        policyAcceptance: policyConsent.acceptance,
      });
    }
  }

  return (
    <Modal
      show={show}
      aria-labelledby="auth-gate-title"
      onHide={onClose}
      centered
      size="sm"
    >
      <Modal.Header closeButton>
        <Modal.Title id="auth-gate-title" className="fs-6">진행 방식 선택</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Nav variant="underline" className="mb-3">
          <Nav.Item>
            <Nav.Link active={tab === "login"} onClick={() => setTab("login")}>로그인</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link active={tab === "signup"} onClick={() => setTab("signup")}>회원가입</Nav.Link>
          </Nav.Item>
          <Nav.Item>
            <Nav.Link active={tab === "guest"} onClick={() => setTab("guest")}>비회원</Nav.Link>
          </Nav.Item>
        </Nav>

        <ErrorAlert error={error} />

        {tab === "login" && (
          <Form onSubmit={handleLogin}>
            <Form.Group className="mb-2" controlId="gate-login-email">
              <Form.Label className="small">이메일</Form.Label>
              <Form.Control
                type="email" size="sm" autoComplete="username" value={email}
                onChange={(e) => setEmail(e.target.value)} required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="gate-login-password">
              <Form.Label className="small">비밀번호</Form.Label>
              <Form.Control
                type="password" size="sm" autoComplete="current-password" value={password}
                onChange={(e) => setPassword(e.target.value)} required minLength={8}
                maxLength={PASSWORD_MAX_UTF8_BYTES}
              />
              <PasswordLengthFeedback value={password} />
            </Form.Group>
            <Button
              type="submit"
              className="w-100"
              size="sm"
              disabled={submitting || !isPasswordWithinByteLimit(password)}
            >
              {submitting ? "로그인 중..." : "로그인 후 내용 확인"}
            </Button>
          </Form>
        )}

        {tab === "signup" && (
          <Form onSubmit={handleSignup}>
            <Form.Group className="mb-2" controlId="gate-signup-email">
              <Form.Label className="small">이메일</Form.Label>
              <Form.Control
                type="email" size="sm" autoComplete="email" value={email}
                onChange={(e) => setEmail(e.target.value)} required
              />
            </Form.Group>
            <Form.Group className="mb-2" controlId="gate-signup-password">
              <Form.Label className="small">비밀번호</Form.Label>
              <Form.Control
                type="password" size="sm" autoComplete="new-password" value={password}
                onChange={(e) => setPassword(e.target.value)} required minLength={8}
                maxLength={PASSWORD_MAX_UTF8_BYTES}
              />
              <PasswordLengthFeedback value={password} />
            </Form.Group>
            <Form.Group className="mb-2" controlId="gate-signup-name">
              <Form.Label className="small">이름</Form.Label>
              <Form.Control
                size="sm" autoComplete="name" value={signupName}
                onChange={(e) => setSignupName(e.target.value)} required
              />
            </Form.Group>
            <div className="mb-3">
              <PhoneVerificationStep
                purpose="SIGNUP"
                title="휴대폰 번호 인증"
                initialPhone={signupPhone}
                confirmLabel="입력 완료"
                onVerified={(phone, code) => {
                  setSignupPhone(normalizePhone(phone));
                  setSignupVerificationCode(code);
                }}
                onReset={() => setSignupVerificationCode("")}
              />
            </div>
            <PolicyConsentFields id="gate-signup-policy-consent" consent={policyConsent} />
            <Button
              type="submit"
              className="w-100"
              size="sm"
              disabled={
                !signupVerificationCode
                || !policyConsent.ready
                || !isPasswordWithinByteLimit(password)
                || submitting
              }
            >
              {submitting ? "가입 중..." : "가입 후 내용 확인"}
            </Button>
          </Form>
        )}

        {tab === "guest" && (
          <div>
            {!guestVerified && guestVerificationReset.message && (
              <Alert variant="info">{guestVerificationReset.message}</Alert>
            )}
            {!guestVerified ? (
              <PhoneVerificationStep
                key={guestVerificationReset.version}
                title="휴대폰 인증"
                initialPhone={guestPhone}
                purpose="GUEST_BOOKING"
                onVerified={(p, c) => {
                  setGuestPhone(p);
                  setGuestCode(c);
                  setGuestVerified(true);
                }}
              />
            ) : (
              <>
                <Form.Group className="mb-3" controlId="gate-guest-name">
                  <Form.Label className="small">이름</Form.Label>
                  <Form.Control
                    size="sm" autoComplete="name" value={guestName}
                    onChange={(e) => setGuestName(e.target.value)}
                    onBlur={() => setGuestNameTouched(true)}
                    placeholder="이름"
                    isInvalid={guestNameTouched && !normalizedGuestName}
                    aria-invalid={guestNameTouched && !normalizedGuestName}
                    aria-describedby={
                      guestNameTouched && !normalizedGuestName
                        ? "gate-guest-name-error"
                        : undefined
                    }
                  />
                  <Form.Control.Feedback id="gate-guest-name-error" type="invalid">
                    이름을 입력해 주세요.
                  </Form.Control.Feedback>
                </Form.Group>
                <PolicyConsentFields id="gate-guest-policy-consent" consent={policyConsent} />
                <Button
                  variant="primary" className="w-100" size="sm"
                  disabled={!normalizedGuestName || !policyConsent.ready}
                  onClick={handleGuestSubmit}
                >
                  비회원으로 진행
                </Button>
                <Button variant="link" size="sm" className="mt-2"
                  onClick={() => { setGuestVerified(false); setGuestCode(""); }}>
                  휴대폰 인증 다시 하기
                </Button>
              </>
            )}
          </div>
        )}
      </Modal.Body>
    </Modal>
  );
}
