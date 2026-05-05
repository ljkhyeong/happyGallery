import { useState } from "react";
import { Container, Form, Button, Alert, Card, Row, Col, Badge } from "react-bootstrap";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "@/shared/api";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { normalizePhone } from "@/shared/validation/phone";
import { SESSION_KEYS } from "@/shared/storage/sessionKeys";

export function SignupPage() {
  const { signup } = useCustomerAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const redirectTo = searchParams.get("redirect") ?? "/";
  const claimIntent = searchParams.get("claim") === "1" || redirectTo.includes("claim=1");
  const loginHref = buildAuthPageHref("/login", {
    redirectTo,
    claim: claimIntent,
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState(searchParams.get("name") ?? "");
  const [phone, setPhone] = useState(normalizePhone(searchParams.get("phone") ?? ""));
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const ok = await signup(email, password, name, phone);
    setSubmitting(false);
    if (ok) {
      navigate(redirectTo);
    } else {
      setError("회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.");
    }
  }

  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 980 }}>
      <Row className="g-4 align-items-stretch">
        <Col lg={5}>
          <Card className="auth-hero-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5 d-flex flex-column">
              <Badge bg="light" text="dark" className="auth-kicker mb-3">
                {claimIntent ? "Claim Onboarding" : "Member Signup"}
              </Badge>
              <h2 className="mb-3">
                {claimIntent
                  ? "같은 번호의 비회원 이력을 회원 계정으로 연결할 준비가 됐습니다"
                  : "회원가입하고 storefront와 예약 흐름을 한 번에 관리하세요"}
              </h2>
              <p className="text-muted-soft mb-4">
                {claimIntent
                  ? "가입이 끝나면 `/my`로 이동하고, 비회원 이력 가져오기를 바로 이어서 진행할 수 있습니다."
                  : "회원가입 후 `/my`에서 주문, 예약, 8회권을 한 곳에서 확인하고 관리할 수 있습니다."}
              </p>
              {claimIntent && (name || phone) && (
                <div className="auth-prefill-card mb-4">
                  <div className="auth-prefill-title">가져온 정보</div>
                  <div className="small text-muted-soft">
                    {name ? <div>이름: {name}</div> : null}
                    {phone ? <div>전화번호: {phone}</div> : null}
                  </div>
                </div>
              )}
              <div className="auth-benefit-list mb-4">
                <div className="auth-benefit-item">회원 주문, 예약, 8회권 전체 목록과 필터 제공</div>
                <div className="auth-benefit-item">비회원 이력 claim과 후속 조회를 `/my`에서 관리</div>
                <div className="auth-benefit-item">상품 상세, 예약, 8회권 흐름에서 같은 세션 유지</div>
              </div>
              <div className="d-flex flex-wrap gap-3 mt-auto small">
                <Link to="/products" className="auth-inline-link">스토어 둘러보기</Link>
                <Link to="/bookings/new" className="auth-inline-link">체험 예약 보기</Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
        <Col lg={7}>
          <Card className="auth-form-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5">
              <h3 className="mb-3">회원가입</h3>
              {error && <Alert variant="danger">{error}</Alert>}
              {claimIntent && (
                <Alert variant="info">
                  같은 휴대폰 번호로 가입하면 <strong>내 정보</strong>로 이동한 뒤 비회원 이력 가져오기를 바로 이어서 진행할 수 있습니다.
                </Alert>
              )}
              {!claimIntent && (
                <p className="text-muted-soft small mb-4">
                  가입이 끝나면 storefront 주문, 예약, 8회권 흐름이 같은 회원 세션으로 이어집니다.
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
                  <Form.Text className="text-muted">8자 이상 입력하세요.</Form.Text>
                </Form.Group>
                <Form.Group className="mb-3" controlId="name">
                  <Form.Label>이름</Form.Label>
                  <Form.Control
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                  />
                </Form.Group>
                <Form.Group className="mb-3" controlId="phone">
                  <Form.Label>전화번호</Form.Label>
                  <Form.Control
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(normalizePhone(e.target.value))}
                    required
                    placeholder="01012345678"
                    maxLength={11}
                  />
                </Form.Group>
                <Button type="submit" className="w-100" disabled={submitting}>
                  {submitting ? "가입 중..." : "회원가입"}
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
                  sessionStorage.setItem(SESSION_KEYS.socialLoginReturnTo, redirectTo);
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
                Google로 회원가입
              </Button>
              <div className="auth-footer-link mt-4">
                이미 계정이 있으신가요? <Link to={loginHref}>로그인</Link>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
