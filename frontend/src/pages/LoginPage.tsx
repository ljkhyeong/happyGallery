import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button, Alert } from "react-bootstrap";
import { Link, useNavigate } from "react-router";
import { AuthPageShell, useAuthPageTarget } from "@/features/customer-auth/AuthPageShell";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { SocialLoginButtons } from "@/features/customer-auth/SocialLoginButtons";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { PasswordInput } from "@/features/customer-auth/PasswordInput";
import { ErrorAlert } from "@/shared/ui";
import { isPasswordWithinByteLimit, PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";

export function LoginPage() {
  const { login } = useCustomerAuth();
  const navigate = useNavigate();
  const { returnTo, claimIntent } = useAuthPageTarget();
  const signupHref = buildAuthPageHref("/signup", {
    redirectTo: returnTo,
    claim: claimIntent,
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const loginMutation = useMutation({
    mutationFn: () => login(email, password),
    onSuccess: () => navigate(returnTo),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isPasswordWithinByteLimit(password)) return;
    loginMutation.mutate();
  }

  return (
    <AuthPageShell
      title="로그인"
      social={<SocialLoginButtons action="로그인" returnTo={returnTo} />}
      footer={<>계정이 없으신가요? <Link to={signupHref}>회원가입</Link></>}
      hero={{
        kicker: claimIntent ? "비회원 주문·예약 가져오기" : "회원 로그인",
        title: claimIntent
          ? "비회원 주문·예약을 내 정보로 가져오세요"
          : "로그인하고 내 주문·예약·이용권을 확인하세요",
        description: claimIntent
          ? "로그인 후 내 정보에서 가져올 주문·예약을 선택하세요."
          : "로그인하면 추가 인증 없이 이용 내역을 확인할 수 있습니다.",
        benefits: ["주문, 예약, 이용권을 한 화면에서 관리", "같은 번호의 비회원 주문·예약 가져오기"],
        links: (
          <>
            <Link to="/guest/orders" className="auth-inline-link">비회원 주문 조회</Link>
            <Link to="/guest/bookings" className="auth-inline-link">비회원 예약 조회</Link>
          </>
        ),
      }}
    >
      <ErrorAlert error={loginMutation.error} />
      {claimIntent && (
        <Alert variant="info">
          로그인 후 <strong>내 정보</strong>에서 비회원 주문과 예약을 바로 가져올 수 있습니다.
        </Alert>
      )}
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3" controlId="email">
          <Form.Label>이메일</Form.Label>
          <Form.Control
            type="email"
            autoComplete="username"
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
          <PasswordInput
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={PASSWORD_MAX_UTF8_BYTES}
          />
          <PasswordLengthFeedback value={password} />
        </Form.Group>
        <Button
          type="submit"
          className="w-100"
          disabled={loginMutation.isPending || !isPasswordWithinByteLimit(password)}
        >
          {loginMutation.isPending ? "로그인 중..." : "로그인"}
        </Button>
      </Form>
    </AuthPageShell>
  );
}
