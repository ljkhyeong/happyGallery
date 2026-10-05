import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import type { ClassResponse } from "@/generated/api/booking";
import {
  bookingCreateHref,
  formatSlotStart,
  remainingSeatLabel,
  UPCOMING_SLOT_DAYS,
  upcomingSlotsQuery,
} from "./upcomingSlots";

const SLOT_LIMIT = 8;

/** 클래스 상세에서 남은 자리가 있는 가까운 회차를 고르면 예약 화면이 그 시간을 선택한 채로 열린다. */
export function ClassUpcomingSlots({ bookingClass }: { bookingClass: Pick<ClassResponse, "id" | "name"> }) {
  const query = useQuery(upcomingSlotsQuery(bookingClass.id));
  const openSlots = (query.data ?? [])
    .filter((slot) => slot.remainingCapacity > 0)
    .sort((left, right) => left.startAt.localeCompare(right.startAt));

  return (
    <section className="class-detail-section" aria-labelledby="class-upcoming-title">
      <div className="class-detail-section-head">
        <h2 id="class-upcoming-title">다가오는 일정</h2>
        <span>앞으로 {UPCOMING_SLOT_DAYS}일</span>
      </div>
      {query.isPending && <p className="class-detail-muted">일정을 불러오는 중입니다.</p>}
      {query.isError && (
        <p className="class-detail-muted">
          일정을 불러오지 못했습니다. <Link to={bookingCreateHref(bookingClass.id)}>예약 화면에서 날짜 보기 →</Link>
        </p>
      )}
      {query.isSuccess && openSlots.length === 0 && (
        <p className="class-detail-muted">
          지금 예약할 수 있는 일정이 없습니다. 만석인 날은 예약 화면에서 빈자리 알림을 신청할 수 있어요.
        </p>
      )}
      {openSlots.length > 0 && (
        <ul className="class-slot-chips">
          {openSlots.slice(0, SLOT_LIMIT).map((slot) => (
            <li key={slot.id}>
              <Link
                to={bookingCreateHref(bookingClass.id, slot.id, { selectSlot: true })}
                aria-label={`${formatSlotStart(slot)} ${bookingClass.name} 예약하기, ${remainingSeatLabel(slot.remainingCapacity)}`}
              >
                <strong>{formatSlotStart(slot)}</strong>
                <small className={slot.remainingCapacity <= 2 ? "is-few" : undefined}>
                  {remainingSeatLabel(slot.remainingCapacity)}
                </small>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {openSlots.length > SLOT_LIMIT && (
        <Link className="class-detail-more" to={bookingCreateHref(bookingClass.id)}>
          다른 날짜 더 보기 →
        </Link>
      )}
    </section>
  );
}
