import { formatKRW } from "@/shared/lib";
import type { BookingDetailResponse } from "@/shared/types";
import { BookingInfoCard } from "./BookingInfoCard";

interface Props {
  booking: BookingDetailResponse;
}

export function BookingDetail({ booking }: Props) {
  return (
    <BookingInfoCard
      label={booking.bookingNumber}
      status={booking.status}
      className={booking.className}
      startAt={booking.startAt}
      endAt={booking.endAt}
      refund={booking.refund}
      rows={[
        { label: "예약자", value: `${booking.guestName} (${booking.guestPhone})` },
        { label: "예약 인원", value: `${booking.participantCount}명` },
        { label: "예약금", value: formatKRW(booking.depositAmount) },
        { label: "현장 잔금", value: formatKRW(booking.balanceAmount) },
      ]}
    />
  );
}
