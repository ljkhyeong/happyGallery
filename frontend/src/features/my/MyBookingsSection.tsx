import { Link } from "react-router";
import { EmptyState, ErrorAlert, LoadingSpinner, StatusBadge } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { MyBookingSummary } from "./api";
import { bookingDepositLabel } from "./listUtils";

interface Props {
  previewSize?: number;
  bookings: MyBookingSummary[] | undefined;
  isLoading: boolean;
  error: Error | null;
  isFetching: boolean;
  onRetry: () => void;
}

export function MyBookingsSection({ bookings, isLoading, error, isFetching, onRetry, previewSize = 3 }: Props) {
  return (
    <section id="my-bookings" className="my-recent" aria-labelledby="my-bookings-title">
      <div className="my-recent-head">
        <h2 id="my-bookings-title">최근 예약</h2>
        <Link to="/my/bookings">전체 보기 →</Link>
      </div>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} onRetry={onRetry} retrying={isFetching} />
      {bookings && bookings.length === 0 && <EmptyState message="예약 내역이 없습니다." />}
      {bookings?.slice(0, previewSize).map((booking) => (
        <Link key={booking.bookingId} to={`/my/bookings/${booking.bookingId}`} className="my-list-card my-recent-item">
          <div className="my-recent-main">
            <strong>{booking.className}</strong>
            <small>{formatDateTime(booking.startAt)} · {booking.participantCount}명</small>
          </div>
          <div className="my-recent-side">
            <StatusBadge status={booking.status} />
            <span>{bookingDepositLabel(booking.depositAmount)}</span>
          </div>
        </Link>
      ))}
    </section>
  );
}
