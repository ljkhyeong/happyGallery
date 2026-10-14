import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Card, Container, Form } from "react-bootstrap";
import { Link, useLocation, useNavigate } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { resetPassword } from "@/features/customer-auth/credentialApi";
import { useAuthPageTarget } from "@/features/customer-auth/AuthPageShell";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { NewPasswordFields } from "@/features/customer-auth/NewPasswordFields";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert, useToast } from "@/shared/ui";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function ForgotPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const { refresh } = useCustomerAuth();
  // 로그인 화면에서 넘어온 돌아갈 곳·가져오기 의도를 재설정 뒤 로그인까지 이어 간다.
  const { returnTo, claimIntent } = useAuthPageTarget();
  const loginHref = buildAuthPageHref("/login", { redirectTo: returnTo, claim: claimIntent });
  const prefill = location.state as { email?: string; phone?: string | null } | null;
  const [email, setEmail] = useState(prefill?.email ?? "");
  const [newPassword, setNewPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");

  const mutation = useMutation({
    mutationFn: ({ phone, verificationCode }: { phone: string; verificationCode: string }) =>
      resetPassword(email.trim(), phone, verificationCode, newPassword),
    onSuccess: async () => {
      const currentUser = await refresh();
      toast.show("비밀번호가 설정되었습니다.");
      navigate(currentUser ? (returnTo !== "/" ? returnTo : "/my") : loginHref, { replace: true });
    },
  });

  const passwordMatches = newPassword === confirmation;
  const detailsValid = EMAIL_PATTERN.test(email.trim())
    && newPassword.length >= 8
    && isPasswordWithinByteLimit(newPassword)
    && passwordMatches;

  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 560 }}>
      <Card className="auth-form-card border-0">
        <Card.Body className="p-4 p-lg-5">
          <h1 className="h3 display-heading mb-2">비밀번호 재설정</h1>
          <p className="text-muted-soft small mb-4">
            가입한 이메일과 인증한 휴대폰 번호를 입력하세요.
          </p>
          <ErrorAlert error={mutation.error} />

          <Form.Group className="mb-3" controlId="reset-email">
            <Form.Label>이메일</Form.Label>
            <Form.Control
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
              autoFocus
            />
          </Form.Group>
          <NewPasswordFields
            idPrefix="reset-password"
            password={newPassword}
            confirmation={confirmation}
            onPasswordChange={setNewPassword}
            onConfirmationChange={setConfirmation}
            confirmationClassName="mb-4"
          />

          <PhoneVerificationStep
            purpose="PASSWORD_RESET"
            title="휴대폰 인증"
            initialPhone={prefill?.phone ?? ""}
            confirmLabel="인증하고 재설정"
            confirming={mutation.isPending}
            confirmDisabled={!detailsValid}
            onReset={() => mutation.reset()}
            onVerified={(phone, verificationCode) =>
              mutation.mutate({ phone, verificationCode })
            }
          />

          <div className="auth-footer-link mt-4">
            <Link to={loginHref}>로그인으로 돌아가기</Link>
          </div>
        </Card.Body>
      </Card>
    </Container>
  );
}
