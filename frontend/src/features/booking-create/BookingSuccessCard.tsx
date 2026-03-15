import { Card, Alert } from "react-bootstrap";
import { GuestClaimSuccessActions } from "@/features/customer-claim/GuestClaimSuccessActions";
import { StatusBadge } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";
import type { BookingResponse } from "@/shared/types";

interface Props {
  booking: BookingResponse;
  guestPhone?: string;
  guestName?: string;
}

export function BookingSuccessCard({ booking, guestPhone, guestName }: Props) {
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
          <GuestClaimSuccessActions
            primaryTo="/guest/bookings"
            primaryLabel="비회원 예약 조회"
            guestPhone={guestPhone}
            guestName={guestName}
            helperText="같은 휴대폰 번호로 회원가입하거나 로그인하면 `/my`에서 기존 비회원 예약을 바로 가져올 수 있습니다."
          />
        </Card.Footer>
      </Card>
    </div>
  );
}
