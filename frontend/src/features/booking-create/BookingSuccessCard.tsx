import { Card, Alert } from "react-bootstrap";
import { Link } from "react-router-dom";
import { StatusBadge } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingResponse } from "@/shared/types";

interface Props {
  booking: BookingResponse;
}

export function BookingSuccessCard({ booking }: Props) {
  return (
    <div>
      <Alert variant="success" className="mb-3">
        예약이 완료되었습니다!
      </Alert>
      <Card>
        <Card.Body>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="mb-0">{booking.bookingNumber}</h5>
            <StatusBadge status={booking.status} />
          </div>
          <p className="mb-1">
            <strong>클래스:</strong> {booking.className}
          </p>
          <p className="mb-1">
            <strong>예약금:</strong> {formatKRW(booking.depositAmount)}
          </p>
          <p className="mb-1">
            <strong>잔금:</strong> {formatKRW(booking.balanceAmount)}
          </p>
          <Alert variant="warning" className="mt-3 mb-0">
            <small>
              <strong>Access Token:</strong>{" "}
              <code>{booking.accessToken}</code>
              <br />
              이 토큰은 예약 조회/변경/취소에 필요합니다. 반드시 보관하세요.
            </small>
          </Alert>
        </Card.Body>
        <Card.Footer>
          <Link to="/guest/bookings" className="text-decoration-none small">
            예약 조회 페이지로 이동 &rarr;
          </Link>
        </Card.Footer>
      </Card>
    </div>
  );
}
