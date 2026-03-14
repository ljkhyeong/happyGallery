import { useState } from "react";
import { Container, Form, Button, Alert } from "react-bootstrap";
import { Link, useNavigate } from "react-router-dom";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";

export function SignupPage() {
  const { signup } = useCustomerAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    const ok = await signup(email, password, name, phone);
    setSubmitting(false);
    if (ok) {
      navigate("/");
    } else {
      setError("회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있습니다.");
    }
  }

  return (
    <Container style={{ maxWidth: 420 }}>
      <h2 className="mb-4">회원가입</h2>
      {error && <Alert variant="danger">{error}</Alert>}
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
            onChange={(e) => setPhone(e.target.value)}
            required
            placeholder="010-0000-0000"
          />
        </Form.Group>
        <Button type="submit" className="w-100" disabled={submitting}>
          {submitting ? "가입 중..." : "회원가입"}
        </Button>
      </Form>
      <p className="mt-3 text-center">
        이미 계정이 있으신가요? <Link to="/login">로그인</Link>
      </p>
    </Container>
  );
}
