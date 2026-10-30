import { PasswordLengthFeedback } from "@/shared/ui/PasswordLengthFeedback";
import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Button, Card, Container, Form, Spinner } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { isPasswordWithinByteLimit, PASSWORD_MAX_UTF8_BYTES } from "@/shared/validation/password";
import type { AdminAuthResponse } from "./api";

interface Props {
  onLogin: (username: string, password: string) => Promise<AdminAuthResponse>;
  onVerifyMfa: (challengeToken: string, code: string) => Promise<AdminAuthResponse>;
}

export function AdminLoginGate({ onLogin, onVerifyMfa }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [challengeToken, setChallengeToken] = useState<string>();
  const [code, setCode] = useState("");
  const normalizedUsername = username.trim();
  const normalizedCode = code.trim();

  // 로그인 전 화면이라 세션 만료 처리가 없다. INVALID_CREDENTIALS 등 실패는 오류 안내로만 보인다.
  const credentialLogin = useMutation({
    mutationFn: () => onLogin(normalizedUsername, password),
    onSuccess: (result) => {
      if (result.status === "MFA_REQUIRED" && result.challengeToken) {
        setChallengeToken(result.challengeToken);
        setPassword("");
      }
    },
  });
  const mfaVerification = useMutation({
    mutationFn: (token: string) => onVerifyMfa(token, normalizedCode),
    onSuccess: (result) => {
      if (result.status === "MFA_REQUIRED" && result.challengeToken) {
        setChallengeToken(result.challengeToken);
        setCode("");
      }
    },
  });
  const loading = credentialLogin.isPending || mfaVerification.isPending;
  const error = challengeToken ? mfaVerification.error : credentialLogin.error;

  function submitCredentials(event: React.FormEvent) {
    event.preventDefault();
    if (!normalizedUsername
      || !password
      || !isPasswordWithinByteLimit(password)
      || loading) return;
    credentialLogin.mutate();
  }

  function submitMfa(event: React.FormEvent) {
    event.preventDefault();
    if (!challengeToken || !normalizedCode || loading) return;
    mfaVerification.mutate(challengeToken);
  }

  function returnToCredentials() {
    setChallengeToken(undefined);
    setCode("");
    mfaVerification.reset();
  }

  return (
    <Container className="page-container" style={{ maxWidth: 400 }}>
      <Card>
        <Card.Body>
          <h5 className="mb-3">
            {challengeToken ? "2단계 인증" : "관리자 로그인"}
          </h5>
          <ErrorAlert error={error} />

          {challengeToken ? (
            <Form onSubmit={submitMfa} aria-busy={loading}>
              <Form.Group controlId="admin-mfa-code" className="mb-3">
                <Form.Label>인증 코드 또는 복구 코드</Form.Label>
                <Form.Control
                  type="text"
                  inputMode="text"
                  autoComplete="one-time-code"
                  maxLength={32}
                  value={code}
                  onChange={(event) => setCode(event.target.value)}
                  autoFocus
                  disabled={loading}
                />
              </Form.Group>
              <div className="d-flex gap-2">
                <Button
                  type="submit"
                  variant="primary"
                  disabled={!normalizedCode || loading}
                >
                  {loading ? (
                    <>
                      <Spinner size="sm" aria-hidden="true" />
                      <span className="visually-hidden">인증 확인 중...</span>
                    </>
                  ) : "확인"}
                </Button>
                <Button
                  type="button"
                  variant="outline-secondary"
                  onClick={returnToCredentials}
                  disabled={loading}
                >
                  이전
                </Button>
              </div>
            </Form>
          ) : (
            <Form onSubmit={submitCredentials} aria-busy={loading}>
              <Form.Group controlId="admin-username" className="mb-3">
                <Form.Label>아이디</Form.Label>
                <Form.Control
                  type="text"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="관리자 아이디"
                  autoComplete="username"
                  autoFocus
                  disabled={loading}
                />
              </Form.Group>
              <Form.Group controlId="admin-password" className="mb-3">
                <Form.Label>비밀번호</Form.Label>
                <Form.Control
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="비밀번호"
                  autoComplete="current-password"
                  maxLength={PASSWORD_MAX_UTF8_BYTES}
                  disabled={loading}
                />
                <PasswordLengthFeedback value={password} />
              </Form.Group>
              <Button
                type="submit"
                variant="primary"
                disabled={
                  !normalizedUsername
                  || !password
                  || !isPasswordWithinByteLimit(password)
                  || loading
                }
              >
                {loading ? (
                  <>
                    <Spinner size="sm" aria-hidden="true" />
                    <span className="visually-hidden">로그인 중...</span>
                  </>
                ) : "로그인"}
              </Button>
            </Form>
          )}
        </Card.Body>
      </Card>
    </Container>
  );
}
