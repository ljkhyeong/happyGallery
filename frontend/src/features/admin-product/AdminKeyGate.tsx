import { useState } from "react";
import { Card, Form, Button, Alert, Spinner } from "react-bootstrap";

interface Props {
  onLogin: (username: string, password: string) => Promise<boolean>;
}

export function AdminKeyGate({ onLogin }: Props) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const normalizedUsername = username.trim();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!normalizedUsername || !password) return;
    setError("");
    setLoading(true);
    const ok = await onLogin(normalizedUsername, password);
    setLoading(false);
    if (!ok) {
      setError("아이디 또는 비밀번호가 올바르지 않습니다.");
    }
  };

  return (
    <div className="page-container" style={{ maxWidth: 400 }}>
      <Card>
        <Card.Body>
          <h5 className="mb-3">관리자 로그인</h5>
          {error && <Alert variant="danger">{error}</Alert>}
          <Form onSubmit={handleSubmit}>
            <Form.Group controlId="admin-username" className="mb-3">
              <Form.Label>아이디</Form.Label>
              <Form.Control
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="관리자 아이디"
                autoFocus
                disabled={loading}
              />
            </Form.Group>
            <Form.Group controlId="admin-password" className="mb-3">
              <Form.Label>비밀번호</Form.Label>
              <Form.Control
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="비밀번호"
                disabled={loading}
              />
            </Form.Group>
            <Button
              type="submit"
              variant="primary"
              disabled={!normalizedUsername || !password || loading}
            >
              {loading ? <Spinner size="sm" /> : "로그인"}
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}
