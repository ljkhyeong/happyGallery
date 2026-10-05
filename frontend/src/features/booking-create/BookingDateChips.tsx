import { formatDate, formatDateTime, formatTime } from "@/shared/lib";
import type { PublicSlotResponse } from "@/shared/types";
import { slotDayParts } from "./upcomingSlots";

interface Props {
  label: string;
  dates: readonly string[];
  activeDate: string;
  onSelect: (date: string) => void;
  /** 모든 회차가 만석인 날은 고를 수는 있게 두고 '마감'으로 표시한다. */
  isFull?: (date: string) => boolean;
  disabled?: boolean;
}

/** 예약·예약 변경 화면이 함께 쓰는 날짜 칩. 선택은 aria-pressed로 알린다. */
export function BookingDateChips({ label, dates, activeDate, onSelect, isFull, disabled = false }: Props) {
  const monthLabel = Array.from(new Set(dates.map((value) => `${slotDayParts(value).month}월`))).join(" · ");
  return (
    <div className="booking-date-step">
      <p className="booking-step-label">{monthLabel}</p>
      <div className="booking-date-chips" role="group" aria-label={label}>
        {dates.map((value) => {
          const { month, day, weekday } = slotDayParts(value);
          const full = isFull?.(value) ?? false;
          return (
            <button
              key={value}
              type="button"
              data-booking-date={value}
              aria-pressed={value === activeDate}
              aria-label={`${month}월 ${day}일 ${weekday}요일${full ? " 마감" : ""}`}
              className={full ? "is-full" : undefined}
              disabled={disabled}
              onClick={() => onSelect(value)}
            >
              <span>{weekday}</span>
              <b>{day}</b>
              {full && <small>마감</small>}
            </button>
          );
        })}
      </div>
    </div>
  );
}

/** 위에서 고른 날짜의 회차만 보여 주므로 시간만 표시한다. 날짜는 보조기기용 숨김 텍스트로 붙이고, 다음 날 끝나는 수업만 종료 날짜를 보인다. */
export function BookingSlotTime({ slot }: { slot: Pick<PublicSlotResponse, "startAt" | "endAt"> }) {
  const sameDay = slot.endAt.slice(0, 10) === slot.startAt.slice(0, 10);
  return (
    <span className="booking-slot-time">
      <span className="visually-hidden">{formatDate(slot.startAt)} </span>
      <b>{formatTime(slot.startAt)}</b>
      <small> ~ {sameDay ? formatTime(slot.endAt) : formatDateTime(slot.endAt)}</small>
    </span>
  );
}
