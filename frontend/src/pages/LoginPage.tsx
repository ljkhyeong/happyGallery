import { useState } from "react";
import { Container, Form, Button, Alert, Card, Row, Col, Badge } from "react-bootstrap";
import { Link, useNavigate, useLocation, useSearchParams } from "react-router-dom";
import { api } from "@/shared/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";

export function LoginPage() {
  const { login } = useCustomerAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const returnTo = searchParams.get("redirect") ?? (location.state as { from?: string } | null)?.from ?? "/";
  const claimIntent = searchParams.get("claim") === "1" || returnTo.includes("claim=1");
  const signupHref = buildAuthPageHref("/signup", {
    redirectTo: returnTo,
    claim: claimIntent,
    phone: searchParams.get("phone") ?? undefined,
    name: searchParams.get("name") ?? undefined,
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const ok = await login(email, password);
    setSubmitting(false);
    if (ok) {
      navigate(returnTo);
    } else {
      setError("이메일 또는 비밀번호가 올바르지 않습니다.");
    }
  }

  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 980 }}>
      <Row className="g-4 align-items-stretch">
        <Col lg={5}>
          <Card className="auth-hero-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5 d-flex flex-column">
              <Badge bg="light" text="dark" className="auth-kicker mb-3">
                {claimIntent ? "Guest Claim Login" : "Member Login"}
              </Badge>
              <h2 className="mb-3">
                {claimIntent
                  ? "기존 비회원 이력을 회원 계정으로 이어서 가져오세요"
                  : "로그인하고 주문, 예약, 8회권을 계속 관리하세요"}
              </h2>
              <p className="text-muted-soft mb-4">
                {claimIntent
                  ? "로그인 후 `/my`로 이동하면 비회원 이력 가져오기 모달이 바로 열립니다."
                  : "회원은 `/my`에서 주문, 예약, 8회권을 추가 인증 없이 바로 확인할 수 있습니다."}
              </p>
              <div className="auth-benefit-list mb-4">
                <div className="auth-benefit-item">주문, 예약, 8회권을 한 화면에서 관리</div>
                <div className="auth-benefit-item">같은 번호의 비회원 이력 claim</div>
                <div className="auth-benefit-item">필요 시 `/guest/**` 조회 경로도 계속 사용 가능</div>
              </div>
              <div className="d-flex flex-wrap gap-3 mt-auto small">
                <Link to="/guest/orders" className="auth-inline-link">비회원 주문 조회</Link>
                <Link to="/guest/bookings" className="auth-inline-link">비회원 예약 조회</Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={7}>
          <Card className="auth-form-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5">
              <h3 className="mb-3">로그인</h3>
              {error && <Alert variant="danger">{error}</Alert>}
              {claimIntent && (
                <Alert variant="info">
                  로그인 후 <strong>내 정보</strong>에서 비회원 주문과 예약을 바로 가져올 수 있습니다.
                </Alert>
              )}
              {!claimIntent && (
                <p className="text-muted-soft small mb-4">
                  지금 로그인하면 최근 주문 상태, 예약 상세, 8회권 잔여 횟수를 `/my`에서 바로 이어서 볼 수 있습니다.
                </p>
              )}
              <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="email">
                  <Form.Label>이메일</Form.Label>
                  <Form.Control
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    autoFocus
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="password">
                  <Form.Label>비밀번호</Form.Label>
                  <Form.Control
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                  />
                </Form.Group>
                <Button type="submit" className="w-100" disabled={submitting}>
                  {submitting ? "로그인 중..." : "로그인"}
                </Button>
              </Form>
              <div className="d-flex align-items-center my-4">
                <hr className="flex-grow-1" />
                <span className="px-3 text-muted-soft small">또는</span>
                <hr className="flex-grow-1" />
              </div>
              <Button
                variant="outline-dark"
                className="w-100"
                onClick={async () => {
                  sessionStorage.setItem(SESSION_KEYS.socialLoginReturnTo, returnTo);
                  const redirectUri = window.location.origin + "/auth/callback/google";
                  try {
                    const data = await api<{ url: string; state: string }>(
                      `/auth/social/google/url?redirectUri=${encodeURIComponent(redirectUri)}`,
                    );
                    sessionStorage.setItem(SESSION_KEYS.googleOauthState, data.state);
                    window.location.href = data.url;
                  } catch {
                    setError("Google 로그인 준비에 실패했습니다.");
                  }
                }}
              >
                Google로 로그인
              </Button>
              <div className="auth-footer-link mt-4">
                계정이 없으신가요? <Link to={signupHref}>회원가입</Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
