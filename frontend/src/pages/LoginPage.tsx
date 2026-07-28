import { useState } from "react";
import { Container, Form, Button, Alert, Card, Row, Col, Badge } from "react-bootstrap";
import { Link, useNavigate, useLocation, useSearchParams } from "react-router";
import { buildAuthPageHref, resolveSafeReturnTo } from "@/features/customer-auth/navigation";
import { SocialLoginButtons } from "@/features/customer-auth/SocialLoginButtons";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert } from "@/shared/ui";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";

export function LoginPage() {
  const { login } = useCustomerAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const returnTo = resolveSafeReturnTo(
    searchParams.get("redirect") ?? (location.state as { from?: string } | null)?.from,
  );
  const claimIntent = searchParams.get("claim") === "1" || returnTo.includes("claim=1");
  const signupHref = buildAuthPageHref("/signup", {
    redirectTo: returnTo,
    claim: claimIntent,
    phone: searchParams.get("phone") ?? undefined,
    name: searchParams.get("name") ?? undefined,
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isPasswordWithinByteLimit(password)) return;
    setError(null);
    setSubmitting(true);
    try {
      await login(email, password);
      navigate(returnTo);
    } catch (requestError) {
      setError(requestError);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 980 }}>
      <Row className="g-4 align-items-stretch">
        <Col lg={5}>
          <Card className="auth-hero-card border-0 h-100">
            <Card.Body className="p-4 p-lg-5 d-flex flex-column">
              <Badge bg="light" text="dark" className="auth-kicker mb-3">
                {claimIntent ? "비회원 이력 가져오기" : "회원 로그인"}
              </Badge>
              <h2 className="mb-3">
                {claimIntent
                  ? "기존 비회원 이력을 회원 계정으로 이어서 가져오세요"
                  : "로그인하고 주문, 예약, 8회권을 계속 관리하세요"}
              </h2>
              <p className="text-muted-soft mb-4">
                {claimIntent
                  ? "로그인 후 내 정보에서 비회원 이력 가져오기를 바로 이어서 진행합니다."
                  : "회원은 내 정보에서 주문, 예약, 8회권을 추가 인증 없이 바로 확인할 수 있습니다."}
              </p>
              <div className="auth-benefit-list mb-4">
                <div className="auth-benefit-item">주문, 예약, 8회권을 한 화면에서 관리</div>
                <div className="auth-benefit-item">같은 번호의 비회원 이력 가져오기</div>
                <div className="auth-benefit-item">기존 비회원 주문과 예약도 계속 조회 가능</div>
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
              <ErrorAlert error={error} />
              {claimIntent && (
                <Alert variant="info">
                  로그인 후 <strong>내 정보</strong>에서 비회원 주문과 예약을 바로 가져올 수 있습니다.
                </Alert>
              )}
              {!claimIntent && (
                <p className="text-muted-soft small mb-4">
                  지금 로그인하면 최근 주문 상태, 예약 상세, 8회권 잔여 횟수를 내 정보에서 바로 이어서 볼 수 있습니다.
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
                  <div className="d-flex justify-content-between align-items-center">
                    <Form.Label>비밀번호</Form.Label>
                    <Link
                      to="/forgot-password"
                      state={{ email }}
                      className="auth-inline-link small mb-2"
                    >
                      비밀번호 재설정
                    </Link>
                  </div>
                  <Form.Control
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    minLength={8}
                    maxLength={72}
                  />
                </Form.Group>
                <Button
                  type="submit"
                  className="w-100"
                  disabled={submitting || !isPasswordWithinByteLimit(password)}
                >
                  {submitting ? "로그인 중..." : "로그인"}
                </Button>
              </Form>
              <div className="d-flex align-items-center my-4">
                <hr className="flex-grow-1" />
                <span className="px-3 text-muted-soft small">또는</span>
                <hr className="flex-grow-1" />
              </div>
              <SocialLoginButtons action="로그인" returnTo={returnTo} />
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
