import { Link } from "react-router";
import { DetailRows, StatusBadge, type DetailRow } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { formatSlotEnd } from "@/features/booking-create/upcomingSlots";
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

/** 회원·비회원 예약 상세가 함께 쓰는 예약 정보 카드. */
export function BookingInfoCard({ label, status, classId, className, startAt, endAt, rows, refund }: Props) {
  return (
    <article className="my-detail-card">
      <header className="my-detail-card-head">
        <span>{label}</span>
        <StatusBadge status={status} />
      </header>
      <div className="my-detail-card-hero">
        <h2><Link to={`/classes/${classId}`}>{className}</Link></h2>
        <p>{formatDateTime(startAt)} ~ {formatSlotEnd(startAt, endAt)}</p>
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
