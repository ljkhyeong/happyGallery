import { useState } from "react";
import { Card, Form, Button } from "react-bootstrap";

interface Props {
  onSubmit: (key: string) => void;
}

export function AdminKeyGate({ onSubmit }: Props) {
  const [input, setInput] = useState("");

  return (
    <div className="page-container" style={{ maxWidth: 400 }}>
      <Card>
        <Card.Body>
          <h5 className="mb-3">관리자 인증</h5>
          <Form
            onSubmit={(e) => {
              e.preventDefault();
              if (input.trim()) onSubmit(input.trim());
            }}
          >
            <Form.Group className="mb-3">
              <Form.Label>Admin Key</Form.Label>
              <Form.Control
                type="password"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="X-Admin-Key 입력"
                autoFocus
              />
            </Form.Group>
            <Button type="submit" variant="primary" disabled={!input.trim()}>
              인증
            </Button>
          </Form>
        </Card.Body>
      </Card>
    </div>
  );
}
