import { Link } from "react-router";
import { DetailRows, StatusBadge, type DetailRow } from "@/shared/ui";
import { formatDateTime, formatTime } from "@/shared/lib";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import type { BookingDetailResponse } from "@/generated/api/booking";
import { AddBookingToCalendarButton } from "./AddBookingToCalendarButton";

interface Props {
  label: string;
  status: BookingDetailResponse["status"];
  classId: number;
  className: string;
  startAt: string;
  endAt: string;
  rows: DetailRow[];
  refund: BookingDetailResponse["refund"];
}

/** 회원·비회원 예약 상세가 함께 쓰는 예약 정보 카드. 같은 날 끝나는 수업은 종료 날짜를 반복하지 않는다. */
export function BookingInfoCard({ label, status, classId, className, startAt, endAt, rows, refund }: Props) {
  const sameDay = endAt.slice(0, 10) === startAt.slice(0, 10);
  return (
    <article className="my-detail-card">
      <header className="my-detail-card-head">
        <span>{label}</span>
        <StatusBadge status={status} />
      </header>
      <div className="my-detail-card-hero">
        <h2><Link to={`/classes/${classId}`}>{className}</Link></h2>
        <p>{formatDateTime(startAt)} ~ {sameDay ? formatTime(endAt) : formatDateTime(endAt)}</p>
      </div>
      <DetailRows rows={rows} />
      <div className="my-detail-card-actions">
        <AddBookingToCalendarButton className={className} startAt={startAt} endAt={endAt} status={status} />
      </div>
      <RefundProgressAlert refund={refund} />
      <div className="my-detail-card-visit">
        <WorkshopVisitInfo />
      </div>
    </article>
  );
}
