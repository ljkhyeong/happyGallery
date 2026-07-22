import { useState } from "react";
import { Form, Button, Row, Col } from "react-bootstrap";

interface Props {
  onLookup: (bookingId: number, token: string) => void;
  isLoading: boolean;
  initialBookingId?: string;
  initialToken?: string;
}

export function BookingLookupForm({ onLookup, isLoading, initialBookingId, initialToken }: Props) {
  const [bookingId, setBookingId] = useState(initialBookingId ?? "");
  const [token, setToken] = useState(initialToken ?? "");
  const [touched, setTouched] = useState({ bookingId: false, token: false });

  const parsedBookingId = Number(bookingId);
  const normalizedToken = token.trim();
  const validBookingId = Number.isSafeInteger(parsedBookingId) && parsedBookingId > 0;
  const valid = validBookingId && normalizedToken.length > 0;

  return (
    <Form
      onSubmit={(e) => {
        e.preventDefault();
        setTouched({ bookingId: true, token: true });
        if (valid) onLookup(parsedBookingId, normalizedToken);
      }}
    >
      <Row className="g-2 align-items-end">
        <Col xs={12} sm={4}>
          <Form.Group controlId="booking-lookup-id">
            <Form.Label>예약 번호</Form.Label>
            <Form.Control
              type="number"
              min={1}
              value={bookingId}
              onChange={(e) => setBookingId(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, bookingId: true }))}
              placeholder="예약 번호"
              isInvalid={touched.bookingId && !validBookingId}
            />
            <Form.Control.Feedback type="invalid">
              유효한 예약 번호를 입력해 주세요.
            </Form.Control.Feedback>
          </Form.Group>
        </Col>
        <Col xs={12} sm={5}>
          <Form.Group controlId="booking-lookup-token">
            <Form.Label>조회 코드</Form.Label>
            <Form.Control
              value={token}
              onChange={(e) => setToken(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, token: true }))}
              placeholder="예약 시 발급된 조회 코드"
              isInvalid={touched.token && !normalizedToken}
            />
            <Form.Control.Feedback type="invalid">
              조회 코드를 입력해 주세요.
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
