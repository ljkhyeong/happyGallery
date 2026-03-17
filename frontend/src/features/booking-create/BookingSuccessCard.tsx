import { useState } from "react";
import { Card, Alert, Button } from "react-bootstrap";
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
  const [copied, setCopied] = useState(false);

  const copyToken = async () => {
    await navigator.clipboard.writeText(booking.accessToken);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

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
              <strong>인증 토큰 (1회 표시)</strong>
              <div className="d-flex align-items-center gap-2 mt-1 mb-1">
                <code className="flex-grow-1 text-break">{booking.accessToken}</code>
                <Button variant="outline-secondary" size="sm" onClick={copyToken}>
                  {copied ? "복사됨" : "복사"}
                </Button>
              </div>
              이 토큰은 예약 조회/변경/취소에 필요하며, 이 화면을 벗어나면 다시 확인할 수 없습니다.
            </small>
          </Alert>
        </Card.Body>
        <Card.Footer>
          <GuestClaimSuccessActions
            primaryTo="/guest/bookings"
            primaryLabel="비회원 예약 조회"
            primaryState={{ bookingId: booking.bookingId, token: booking.accessToken }}
            guestPhone={guestPhone}
            guestName={guestName}
            helperText="같은 휴대폰 번호로 회원가입하거나 로그인하면 `/my`에서 기존 비회원 예약을 바로 가져올 수 있습니다."
            trackingSource="booking_success"
          />
        </Card.Footer>
      </Card>
    </div>
  );
}
