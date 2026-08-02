import { useState } from "react";
import { Modal, Button, Form, Nav } from "react-bootstrap";
import { useCustomerAuth, type CustomerUser } from "./useCustomerAuth";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { normalizePhone } from "@/shared/validation/phone";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
import { ErrorAlert } from "@/shared/ui";
import { PolicyConsentFields } from "@/features/policy-consent/PolicyConsentFields";
import { usePolicyAcceptance } from "@/features/policy-consent/usePolicyAcceptance";
import type { PolicyAcceptance } from "@/features/policy-consent/types";

type AuthPath = "login" | "signup" | "guest";

interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
  policyAcceptance: PolicyAcceptance;
}

interface Props {
  show: boolean;
  onClose: () => void;
  onMemberConfirm: (member: CustomerUser) => void;
  onGuestConfirm: (info: GuestInfo) => void;
}

export function AuthGateModal({ show, onClose, onMemberConfirm, onGuestConfirm }: Props) {
  const { isAuthenticated, login, signup, user } = useCustomerAuth();
  const [tab, setTab] = useState<AuthPath>("login");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [signupName, setSignupName] = useState("");
  const [signupPhone, setSignupPhone] = useState("");
  const [signupVerificationCode, setSignupVerificationCode] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);
  const policyConsent = usePolicyAcceptance();

  // guest step
  const [guestVerified, setGuestVerified] = useState(false);
  const [guestPhone, setGuestPhone] = useState("");
  const [guestCode, setGuestCode] = useState("");
  const [guestName, setGuestName] = useState("");
  const [guestNameTouched, setGuestNameTouched] = useState(false);
  const normalizedGuestName = guestName.trim();

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
          <Button variant="secondary" onClick={onClose}>취소</Button>
          <Button variant="primary" onClick={() => onMemberConfirm(user!)}>확인</Button>
        </Modal.Footer>
      </Modal>
    );
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    if (!isPasswordWithinByteLimit(password)) return;
    setError(null);
    setSubmitting(true);
    try {
      const member = await login(email, password);
      onMemberConfirm(member);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault();
    if (!policyConsent.acceptance || !isPasswordWithinByteLimit(password)) {
      return;
    }
    setError(null);
    setSubmitting(true);
    try {
      const member = await signup(
        email,
        password,
        signupName,
        signupPhone,
        signupVerificationCode,
        policyConsent.acceptance,
      );
      onMemberConfirm(member);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
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
        <Nav variant="tabs" className="mb-3">
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
                type="email" size="sm" value={email}
                onChange={(e) => setEmail(e.target.value)} required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="gate-login-password">
              <Form.Label className="small">비밀번호</Form.Label>
              <Form.Control
                type="password" size="sm" value={password}
                onChange={(e) => setPassword(e.target.value)} required minLength={8}
                maxLength={72}
              />
            </Form.Group>
            <Button
              type="submit"
              className="w-100"
              size="sm"
              disabled={submitting || !isPasswordWithinByteLimit(password)}
            >
              {submitting ? "로그인 중..." : "로그인하고 진행"}
            </Button>
          </Form>
        )}

        {tab === "signup" && (
          <Form onSubmit={handleSignup}>
            <Form.Group className="mb-2" controlId="gate-signup-email">
              <Form.Label className="small">이메일</Form.Label>
              <Form.Control
                type="email" size="sm" value={email}
                onChange={(e) => setEmail(e.target.value)} required
              />
            </Form.Group>
            <Form.Group className="mb-2" controlId="gate-signup-password">
              <Form.Label className="small">비밀번호</Form.Label>
              <Form.Control
                type="password" size="sm" value={password}
                onChange={(e) => setPassword(e.target.value)} required minLength={8}
                maxLength={72}
              />
            </Form.Group>
            <Form.Group className="mb-2" controlId="gate-signup-name">
              <Form.Label className="small">이름</Form.Label>
              <Form.Control
                size="sm" value={signupName}
                onChange={(e) => setSignupName(e.target.value)} required
              />
            </Form.Group>
            <div className="mb-3">
              <PhoneVerificationStep
                purpose="SIGNUP"
                title="휴대폰 소유 확인"
                initialPhone={signupPhone}
                confirmLabel="인증코드 적용"
                onVerified={(phone, code) => {
                  setSignupPhone(normalizePhone(phone));
                  setSignupVerificationCode(code);
                }}
                onReset={() => setSignupVerificationCode("")}
              />
            </div>
            <PolicyConsentFields
              id="gate-signup-policy-consent"
              policy={policyConsent.policyQuery.data}
              checked={policyConsent.accepted}
              onChange={policyConsent.setAccepted}
              isLoading={policyConsent.policyQuery.isLoading}
              error={policyConsent.policyQuery.error}
            />
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
              {submitting ? "가입 중..." : "가입하고 진행"}
            </Button>
          </Form>
        )}

        {tab === "guest" && (
          <div>
            {!guestVerified ? (
              <PhoneVerificationStep
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
                    size="sm" value={guestName}
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
                <PolicyConsentFields
                  id="gate-guest-policy-consent"
                  policy={policyConsent.policyQuery.data}
                  checked={policyConsent.accepted}
                  onChange={policyConsent.setAccepted}
                  isLoading={policyConsent.policyQuery.isLoading}
                  error={policyConsent.policyQuery.error}
                />
                <Button
                  variant="primary" className="w-100" size="sm"
                  disabled={!normalizedGuestName || !policyConsent.ready}
                  onClick={handleGuestSubmit}
                >
                  비회원으로 진행
                </Button>
              </>
            )}
          </div>
        )}
      </Modal.Body>
    </Modal>
  );
}
