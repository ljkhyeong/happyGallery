import { useState } from "react";
import { Form, Button, Row, Col } from "react-bootstrap";

interface Props {
  onLookup: (bookingId: number, token: string) => void;
  isLoading: boolean;
}

export function BookingLookupForm({ onLookup, isLoading }: Props) {
  const [bookingId, setBookingId] = useState("");
  const [token, setToken] = useState("");
  const [touched, setTouched] = useState({ bookingId: false, token: false });

  const valid = Number(bookingId) > 0 && token.trim().length > 0;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        setTouched({ bookingId: true, token: true });
        if (valid) onLookup(Number(bookingId), token.trim());
      }}
    >
      <Row className="g-2 align-items-end">
        <Col xs={12} sm={4}>
          <Form.Group>
            <Form.Label>예약 번호</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={bookingId}
              onChange={(e) => setBookingId(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, bookingId: true }))}
              placeholder="예약 ID"
              isInvalid={touched.bookingId && !(Number(bookingId) > 0)}
            />
            <Form.Control.Feedback type="invalid">
              유효한 예약 번호를 입력해 주세요.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={12} sm={5}>
          <Form.Group>
            <Form.Label>인증 토큰</Form.Label>
            <Form.Control
              value={token}
              onChange={(e) => setToken(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, token: true }))}
              placeholder="예약 시 발급된 토큰"
              isInvalid={touched.token && !token.trim()}
            />
            <Form.Control.Feedback type="invalid">
              인증 토큰을 입력해 주세요.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={12} sm={3}>
          <Button type="submit" variant="primary" className="w-100" disabled={!valid || isLoading}>
            {isLoading ? "조회 중..." : "예약 조회"}
          </Button>
        </Col>
      </Row>
    </Form>
  );
}
