import { useState } from "react";
import { Modal, Button, Form, Alert, Nav } from "react-bootstrap";
import { useCustomerAuth } from "./useCustomerAuth";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";

type AuthPath = "login" | "signup" | "guest";

interface GuestInfo {
  phone: string;
  verificationCode: string;
  name: string;
}

interface Props {
  show: boolean;
  onClose: () => void;
  onMemberConfirm: () => void;
  onGuestConfirm: (info: GuestInfo) => void;
}

export function AuthGateModal({ show, onClose, onMemberConfirm, onGuestConfirm }: Props) {
  const { isAuthenticated, login, signup, user } = useCustomerAuth();
  const [tab, setTab] = useState<AuthPath>("login");

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [signupName, setSignupName] = useState("");
  const [signupPhone, setSignupPhone] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // guest step
  const [guestVerified, setGuestVerified] = useState(false);
  const [guestPhone, setGuestPhone] = useState("");
  const [guestCode, setGuestCode] = useState("");
  const [guestName, setGuestName] = useState("");
  const [guestNameTouched, setGuestNameTouched] = useState(false);

  // If already authenticated, confirm directly
  if (isAuthenticated && show) {
    return (
      <Modal show={show} onHide={onClose} centered>
        <Modal.Header closeButton>
          <Modal.Title>확인</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p className="mb-0">
            <strong>{user!.name}</strong>님으로 진행합니다.
          </p>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={onClose}>취소</Button>
          <Button variant="primary" onClick={onMemberConfirm}>확인</Button>
        </Modal.Footer>
      </Modal>
    );
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const ok = await login(email, password);
    setSubmitting(false);
    if (ok) {
      onMemberConfirm();
    } else {
      setError("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  async function handleSignup(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const ok = await signup(email, password, signupName, signupPhone);
    setSubmitting(false);
    if (ok) {
      onMemberConfirm();
    } else {
      setError("회원가입에 실패했습니다. 이미 등록된 이메일일 수 있습니다.");
    }
  }

  function handleGuestSubmit() {
    if (guestName.trim()) {
      onGuestConfirm({ phone: guestPhone, verificationCode: guestCode, name: guestName.trim() });
    }
  }

  return (
    <Modal show={show} onHide={onClose} centered size="sm">
      <Modal.Header closeButton>
        <Modal.Title className="fs-6">진행 방식 선택</Modal.Title>
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

        {error && <Alert variant="danger" className="small">{error}</Alert>}

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
              />
            </Form.Group>
            <Button type="submit" className="w-100" size="sm" disabled={submitting}>
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
              />
            </Form.Group>
            <Form.Group className="mb-2" controlId="gate-signup-name">
              <Form.Label className="small">이름</Form.Label>
              <Form.Control
                size="sm" value={signupName}
                onChange={(e) => setSignupName(e.target.value)} required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="gate-signup-phone">
              <Form.Label className="small">전화번호</Form.Label>
              <Form.Control
                size="sm" value={signupPhone}
                onChange={(e) => setSignupPhone(e.target.value.replace(/\D/g, ""))}
                placeholder="01012345678" maxLength={11} required
              />
            </Form.Group>
            <Button type="submit" className="w-100" size="sm" disabled={submitting}>
              {submitting ? "가입 중..." : "가입하고 진행"}
            </Button>
          </Form>
        )}

        {tab === "guest" && (
          <div>
            {!guestVerified ? (
              <PhoneVerificationStep
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
                    isInvalid={guestNameTouched && !guestName.trim()}
                  />
                  <Form.Control.Feedback type="invalid">
                    이름을 입력해 주세요.
                  </Form.Control.Feedback>
                </Form.Group>
                <Button
                  variant="primary" className="w-100" size="sm"
                  disabled={!guestName.trim()}
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
