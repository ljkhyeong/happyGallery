import { StatusBadge } from "@/shared/ui";
import { BOOKING_BALANCE_STATUS_LABEL, formatDateTime, formatKRW, formatTime } from "@/shared/lib";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { AddBookingToCalendarButton } from "@/features/booking-manage/AddBookingToCalendarButton";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import type { MyBookingDetailResponse } from "@/shared/types";

interface Props {
  booking: MyBookingDetailResponse;
}

export function MyBookingDetailCard({ booking }: Props) {
  // 이용권 예약은 예약금·잔금이 없으므로 결제 방식만 보여 준다.
  const rows = [
    { label: "예약 인원", value: `${booking.participantCount}명` },
    { label: "결제 방식", value: booking.passBooking ? "이용권 사용" : "예약금 결제" },
    ...(booking.passBooking ? [] : [
      { label: "예약금", value: formatKRW(booking.depositAmount) },
      { label: "현장 잔금", value: formatKRW(booking.balanceAmount) },
      { label: "잔금 상태", value: BOOKING_BALANCE_STATUS_LABEL[booking.balanceStatus] ?? "확인 필요" },
    ]),
  ];

  return (
    <article className="my-detail-card">
      <header className="my-detail-card-head">
        <span>예약 #{booking.bookingId}</span>
        <StatusBadge status={booking.status} />
      </header>
      <div className="my-detail-card-hero">
        <h2>{booking.className}</h2>
        <p>
          {formatDateTime(booking.startAt)} ~ {booking.endAt.slice(0, 10) === booking.startAt.slice(0, 10)
            ? formatTime(booking.endAt)
            : formatDateTime(booking.endAt)}
        </p>
      </div>
      <dl className="my-detail-rows">
        {rows.map((row) => (
          <div key={row.label}>
            <dt>{row.label}</dt>
            <dd>{row.value}</dd>
          </div>
        ))}
      </dl>
      <div className="my-detail-card-actions">
        <AddBookingToCalendarButton
          className={booking.className}
          startAt={booking.startAt}
          endAt={booking.endAt}
          status={booking.status}
        />
      </div>
      <RefundProgressAlert refund={booking.refund} />
      <div className="my-detail-card-visit">
        <WorkshopVisitInfo compact />
      </div>
    </article>
  );
}
