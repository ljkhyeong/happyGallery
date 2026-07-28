import { useState } from "react";
import { Button, Card, Form, Spinner } from "react-bootstrap";
import { ErrorAlert } from "@/shared/ui";
import { isPasswordWithinByteLimit } from "@/shared/validation/password";
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
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);
  const normalizedUsername = username.trim();
  const normalizedCode = code.trim();

  async function submitCredentials(event: React.FormEvent) {
    event.preventDefault();
    if (!normalizedUsername
      || !password
      || !isPasswordWithinByteLimit(password)
      || loading) return;

    setError(null);
    setLoading(true);
    try {
      const result = await onLogin(normalizedUsername, password);
      if (result.status === "MFA_REQUIRED" && result.challengeToken) {
        setChallengeToken(result.challengeToken);
        setPassword("");
      }
    } catch (requestError) {
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }

  async function submitMfa(event: React.FormEvent) {
    event.preventDefault();
    if (!challengeToken || !normalizedCode || loading) return;

    setError(null);
    setLoading(true);
    try {
      const result = await onVerifyMfa(challengeToken, normalizedCode);
      if (result.status === "MFA_REQUIRED" && result.challengeToken) {
        setChallengeToken(result.challengeToken);
        setCode("");
      }
    } catch (requestError) {
      setError(requestError);
    } finally {
      setLoading(false);
    }
  }

  function returnToCredentials() {
    setChallengeToken(undefined);
    setCode("");
    setError(null);
  }

  return (
    <div className="page-container" style={{ maxWidth: 400 }}>
      <Card>
        <Card.Body>
          <h5 className="mb-3">
            {challengeToken ? "2단계 인증" : "관리자 로그인"}
          </h5>
          <ErrorAlert error={error} />

          {challengeToken ? (
            <Form onSubmit={submitMfa}>
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
                  {loading ? <Spinner size="sm" /> : "확인"}
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
            <Form onSubmit={submitCredentials}>
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
                  maxLength={72}
                  disabled={loading}
                />
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
                {loading ? <Spinner size="sm" /> : "로그인"}
              </Button>
            </Form>
          )}
        </Card.Body>
      </Card>
    </div>
  );
}
