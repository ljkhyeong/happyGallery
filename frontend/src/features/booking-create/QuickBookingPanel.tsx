import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { classImageSrc, formatKRW, formatTime, getClassCategoryLabel, isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { ClassResponse } from "@/generated/api/booking";
import {
  bookingCreateHref,
  bookingPaymentHint,
  formatSlotStart,
  remainingSeatLabel,
  slotDayParts,
  UPCOMING_SLOT_DAYS,
  upcomingSlotsQuery,
} from "./upcomingSlots";

const CLASS_LIMIT = 3;
const DAY_LIMIT = 6;
const TIME_LIMIT = 6;

/**
 * 홈에서 수업·날짜·시간을 고르면 기존 예약 화면으로 `slotId`를 넘긴다.
 * 결제·인증·정원 확인은 예약 화면이 그대로 처리한다.
 */
export function QuickBookingPanel({ classes }: { classes: ClassResponse[] }) {
  const options = classes.slice(0, CLASS_LIMIT);
  const [classId, setClassId] = useState<number | null>(null);
  const [date, setDate] = useState<string | null>(null);
  const [slotId, setSlotId] = useState<number | null>(null);
  const selectedClass = options.find((option) => option.id === classId) ?? options[0] ?? null;

  const slotsQuery = useQuery({
    ...upcomingSlotsQuery(selectedClass?.id ?? 0),
    enabled: selectedClass !== null,
  });
  const slots = useMemo(
    () => [...(slotsQuery.data ?? [])].sort((left, right) => left.startAt.localeCompare(right.startAt)),
    [slotsQuery.data],
  );
  const dates = useMemo(
    () => Array.from(new Set(slots.map((slot) => slot.startAt.slice(0, 10)))),
    [slots],
  );
  const activeDate = date !== null && dates.includes(date) ? date : (dates[0] ?? null);
  const daySlots = slots.filter((slot) => activeDate !== null && slot.startAt.startsWith(activeDate));
  const activeSlot = daySlots.find((slot) => slot.id === slotId && slot.remainingCapacity > 0)
    ?? daySlots.find((slot) => slot.remainingCapacity > 0)
    ?? null;

  if (!selectedClass) {
    return (
      <aside className="quick-booking" aria-labelledby="quick-booking-title">
        <h2 id="quick-booking-title">바로 예약하기</h2>
        <p className="quick-booking-sub">예약 가능한 클래스를 준비하고 있습니다.</p>
      </aside>
    );
  }

  return (
    <aside className="quick-booking" aria-labelledby="quick-booking-title">
      <h2 id="quick-booking-title">바로 예약하기</h2>
      <p className="quick-booking-sub">수업과 날짜를 고르면 남은 자리를 보여 드립니다.</p>

      <div className="quick-booking-step is-class" role="group" aria-labelledby="quick-booking-class">
        <p id="quick-booking-class" className="quick-booking-step-title">1. 수업</p>
        <div className="quick-booking-classes">
          {options.map((option) => (
            <button
              key={option.id}
              type="button"
              className="quick-booking-class"
              aria-pressed={option.id === selectedClass.id}
              onClick={() => {
                setClassId(option.id);
                setDate(null);
                setSlotId(null);
              }}
            >
              <img src={classImageSrc(option)} alt="" />
              <span>
                <strong>{option.name}</strong>
                <small>
                  {getClassCategoryLabel(option.category)} · {option.durationMin}분
                  {option.passEligible && !isPerfumeClassCategory(option.category) && " · 4회권"}
                </small>
              </span>
              <b>{formatKRW(option.price)}</b>
            </button>
          ))}
        </div>
      </div>

      {slotsQuery.isPending && <p className="quick-booking-state">일정을 불러오는 중입니다.</p>}
      <ErrorAlert
        error={slotsQuery.error}
        onRetry={() => { void slotsQuery.refetch(); }}
        retrying={slotsQuery.isFetching}
      />

      {slotsQuery.isSuccess && dates.length === 0 && (
        <p className="quick-booking-state">
          앞으로 {UPCOMING_SLOT_DAYS}일 안에 예약 가능한 일정이 없습니다.
          {" "}<Link to={`/classes/${selectedClass.id}`}>수업 정보 보기</Link>
        </p>
      )}

      {dates.length > 0 && (
        <>
          <div className="quick-booking-step is-date" role="group" aria-labelledby="quick-booking-date">
            <p id="quick-booking-date" className="quick-booking-step-title">2. 날짜</p>
            <div className="quick-booking-days">
              {dates.slice(0, DAY_LIMIT).map((value) => {
                const { month, day, weekday } = slotDayParts(value);
                return (
                  <button
                    key={value}
                    type="button"
                    aria-pressed={value === activeDate}
                    aria-label={`${month}월 ${day}일 ${weekday}요일`}
                    onClick={() => {
                      setDate(value);
                      setSlotId(null);
                    }}
                  >
                    <span>{weekday}</span>
                    <b>{day}</b>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="quick-booking-step is-time" role="group" aria-labelledby="quick-booking-time">
            <p id="quick-booking-time" className="quick-booking-step-title">3. 시간</p>
            <div className="quick-booking-times">
              {daySlots.slice(0, TIME_LIMIT).map((slot) => (
                <button
                  key={slot.id}
                  type="button"
                  aria-pressed={slot.id === activeSlot?.id}
                  disabled={slot.remainingCapacity === 0}
                  onClick={() => setSlotId(slot.id)}
                >
                  {formatTime(slot.startAt)}
                  <small>{slot.remainingCapacity === 0 ? "마감" : remainingSeatLabel(slot.remainingCapacity)}</small>
                </button>
              ))}
            </div>
            {daySlots.length > TIME_LIMIT && (
              <Link className="quick-booking-more" to={bookingCreateHref(selectedClass.id)}>
                이 날의 다른 시간 {daySlots.length - TIME_LIMIT}개 더 보기
              </Link>
            )}
          </div>
        </>
      )}

      <div className="quick-booking-action">
        {activeSlot ? (
          <LinkButton
            to={bookingCreateHref(selectedClass.id, activeSlot.id, { selectSlot: true })}
            variant="dark"
            size="lg"
            className="w-100"
          >
            {formatSlotStart(activeSlot)} 예약하기
          </LinkButton>
        ) : (
          <LinkButton to={bookingCreateHref(selectedClass.id)} variant="outline-dark" size="lg" className="w-100">
            예약 화면에서 일정 보기
          </LinkButton>
        )}
        <p>{bookingPaymentHint(selectedClass)}</p>
      </div>
    </aside>
  );
}
