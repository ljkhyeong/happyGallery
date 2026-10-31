import { StatusBadge } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { MyBookingSummary } from "./api";
import { bookingDepositLabel } from "./listUtils";
import { MyRecentSection } from "./MyRecentSection";

interface Props {
  bookings: MyBookingSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
}

export function MyBookingsSection({ bookings, ...state }: Props) {
  return (
    <MyRecentSection
      id="my-bookings"
      title="최근 예약"
      listTo="/my/bookings"
      emptyMessage="예약 내역이 없습니다."
      items={bookings}
      {...state}
      itemKey={(booking) => booking.bookingId}
      itemTo={(booking) => `/my/bookings/${booking.bookingId}`}
      renderMain={(booking) => (
        <>
          <strong>{booking.className}</strong>
          <small>{formatDateTime(booking.startAt)} · {booking.participantCount}명</small>
        </>
      )}
      renderSide={(booking) => (
        <>
          <StatusBadge status={booking.status} />
          <span>{bookingDepositLabel(booking.depositAmount)}</span>
        </>
      )}
    />
  );
}
