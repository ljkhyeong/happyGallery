import { BOOKING_BALANCE_STATUS_LABEL, formatKRW } from "@/shared/lib";
import { BookingInfoCard } from "@/features/booking-manage/BookingInfoCard";
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
    <BookingInfoCard
      label={`예약 #${booking.bookingId}`}
      status={booking.status}
      className={booking.className}
      startAt={booking.startAt}
      endAt={booking.endAt}
      refund={booking.refund}
      rows={rows}
    />
  );
}
