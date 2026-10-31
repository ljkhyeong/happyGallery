import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Form, Button } from "react-bootstrap";
import { Link, useNavigate } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { AuthPageShell, useAuthPageTarget } from "@/features/customer-auth/AuthPageShell";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { SocialLoginButtons } from "@/features/customer-auth/SocialLoginButtons";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { PasswordInput } from "@/features/customer-auth/PasswordInput";
import { ErrorAlert } from "@/shared/ui";
import { PolicyConsentFields } from "@/features/policy-consent/PolicyConsentFields";
import { usePolicyAcceptance } from "@/features/policy-consent/usePolicyAcceptance";
import type { PolicyAcceptance } from "@/features/policy-consent/types";
import { isPasswordWithinByteLimit, PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";

export function SignupPage() {
  const { signup } = useCustomerAuth();
  const navigate = useNavigate();
  const { returnTo: redirectTo, claimIntent } = useAuthPageTarget();
  const loginHref = buildAuthPageHref("/login", {
    redirectTo,
    claim: claimIntent,
  });
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const policyConsent = usePolicyAcceptance();
  const signupMutation = useMutation({
    mutationFn: (acceptance: PolicyAcceptance) =>
      signup(email, password, name, phone, verificationCode, acceptance),
    onSuccess: () => navigate(redirectTo),
  });

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!policyConsent.acceptance || !isPasswordWithinByteLimit(password)) {
      return;
    }
    signupMutation.mutate(policyConsent.acceptance);
  }

  return (
    <AuthPageShell
      title="회원가입"
      social={(
        <SocialLoginButtons
          action="회원가입"
          returnTo={redirectTo}
          policyAcceptance={policyConsent.acceptance}
        />
      )}
      footer={<>이미 계정이 있으신가요? <Link to={loginHref}>로그인</Link></>}
      hero={{
        title: claimIntent ? "비회원 주문·예약 가져오기" : "회원만 쓸 수 있는 기능",
        description: claimIntent
          ? "주문·예약 때 쓴 휴대폰 번호로 가입하면 내 정보에서 골라 가져올 수 있습니다."
          : undefined,
        benefits: claimIntent ? undefined : [
          "주문·예약·이용권 내역을 한곳에서 확인",
          "정규반 4회권 구매와 이용권 예약",
          "이벤트 쿠폰과 적립금 사용",
        ],
        links: (
          <>
            <Link to="/products" className="auth-inline-link">작품 보기</Link>
            <Link to="/classes" className="auth-inline-link">클래스 보기</Link>
          </>
        ),
      }}
    >
      <ErrorAlert error={signupMutation.error} />
      <Form onSubmit={handleSubmit}>
        <Form.Group className="mb-3" controlId="email">
          <Form.Label>이메일</Form.Label>
          <Form.Control
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </Form.Group>
        <Form.Group className="mb-3" controlId="password">
          <Form.Label>비밀번호</Form.Label>
          <PasswordInput
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={8}
            maxLength={PASSWORD_MAX_UTF8_BYTES}
          />
          <PasswordLengthFeedback value={password} />
          <Form.Text className="text-muted">
            8자 이상 입력하세요.
          </Form.Text>
        </Form.Group>
        <Form.Group className="mb-3" controlId="name">
          <Form.Label>이름</Form.Label>
          <Form.Control
            type="text"
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </Form.Group>
        <div className="mb-4">
          <PhoneVerificationStep
            purpose="SIGNUP"
            title="휴대폰 번호 인증"
            titleAs="h2"
            initialPhone={phone}
            confirmLabel="입력 완료"
            confirmedNote="인증번호를 입력했습니다. 가입할 때 함께 확인합니다."
            onVerified={(verifiedPhone, code) => {
              setPhone(verifiedPhone);
              setVerificationCode(code);
            }}
            onReset={() => setVerificationCode("")}
          />
        </div>
        <PolicyConsentFields id="signup-policy-consent" consent={policyConsent} />
        <Button
          type="submit"
          className="w-100"
          disabled={
            !verificationCode
            || !policyConsent.ready
            || !isPasswordWithinByteLimit(password)
            || signupMutation.isPending
          }
        >
          {signupMutation.isPending ? "가입 중..." : "회원가입"}
        </Button>
      </Form>
    </AuthPageShell>
  );
}
