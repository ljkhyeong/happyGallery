import { useState } from "react";
import { Form, Button, Row, Col } from "react-bootstrap";

interface Props {
  onLookup: (bookingId: number, token: string) => void;
  isLoading: boolean;
}

export function BookingLookupForm({ onLookup, isLoading }: Props) {
  const [bookingId, setBookingId] = useState("");
  const [token, setToken] = useState("");

  const valid = Number(bookingId) > 0 && token.trim().length > 0;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
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
              placeholder="예약 ID"
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={5}>
          <Form.Group>
            <Form.Label>인증 토큰</Form.Label>
            <Form.Control
              value={token}
              onChange={(e) => setToken(e.target.value)}
              placeholder="예약 시 발급된 토큰"
            />
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
