import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Card, Container, Form } from "react-bootstrap";
import { Link, useLocation, useNavigate } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { resetPassword } from "@/features/customer-auth/credentialApi";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert, useToast } from "@/shared/ui";

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function ForgotPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();
  const { refresh } = useCustomerAuth();
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
      navigate(currentUser ? "/my" : "/login", { replace: true });
    },
  });

  const passwordMatches = newPassword === confirmation;
  const detailsValid = EMAIL_PATTERN.test(email.trim())
    && newPassword.length >= 8
    && newPassword.length <= 100
    && passwordMatches;

  return (
    <Container className="page-container auth-shell" style={{ maxWidth: 560 }}>
      <Card className="auth-form-card border-0">
        <Card.Body className="p-4 p-lg-5">
          <h3 className="mb-2">비밀번호 재설정</h3>
          <p className="text-muted-soft small mb-4">
            가입한 이메일과 검증된 휴대폰 번호를 입력하세요.
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
          <Form.Group className="mb-3" controlId="reset-password">
            <Form.Label>새 비밀번호</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              minLength={8}
              maxLength={100}
              required
            />
            <Form.Text className="text-muted">8자 이상 입력하세요.</Form.Text>
          </Form.Group>
          <Form.Group className="mb-4" controlId="reset-password-confirmation">
            <Form.Label>새 비밀번호 확인</Form.Label>
            <Form.Control
              type="password"
              autoComplete="new-password"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              maxLength={100}
              isInvalid={confirmation.length > 0 && !passwordMatches}
              required
            />
            <Form.Control.Feedback type="invalid">
              새 비밀번호가 일치하지 않습니다.
            </Form.Control.Feedback>
          </Form.Group>

          <PhoneVerificationStep
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
            <Link to="/login">로그인으로 돌아가기</Link>
          </div>
        </Card.Body>
      </Card>
    </Container>
  );
}
